package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.service.FileService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class FileServiceImpl implements FileService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    MongoTemplate mongoTemplate;

    // 定义文件类型后缀的正则表达式
// 修改所有正则：移除结尾锚点 $，添加单词边界 \b
    private static final String IMAGE_REGEX = "\\.(jpe?g|png|gif|bmp|webp|svg|tiff)\\b";
    private static final String VIDEO_REGEX = "\\.(mp4|avi|mov|flv|wmv|mkv)\\b";
    private static final String AUDIO_REGEX = "\\.(mp3|wav|flac|aac)\\b";
    private static final String DOCUMENT_REGEX = "\\.(pdf|docx?|xlsx?|pptx?|txt)\\b";


    @Override
    public File add2(File file) {
        return mongoTemplate.insert(file);
    }

    @Override
    public boolean addToSetFriend(String fileId, ShareFileToUser shareFileToUser) {
        // 定义查询条件，找到需要更新的文档
        Query query = new Query(Criteria.where("id").is(fileId));

        // 使用 $addToSet 操作符，将新对象添加到 items 数组，确保不会重复添加
        Update update = new Update().addToSet("shareToFriends", shareFileToUser);

        // 更新文档
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, File.class);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean deleteFriend(String fileId, String friendId) {
        // 定义查询条件，找到需要更新的文档
        Query query = new Query(Criteria.where("id").is(fileId));

        // 使用 $pull 操作符从 items 数组中移除匹配的对象
        Update update = new Update().pull("shareToFriends", Query.query(Criteria.where("uid").is(friendId)));

        // 更新文档
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, File.class);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean add(File file) throws IOException {
        mongoTemplate.insert(file);
        return true;
    }

    @Override
    public boolean delete(String fileId, boolean isThoroughly) {
        log.info("delete fileId ={}", fileId);
        log.info("delete isThoroughly ={}", isThoroughly);
        Query query = new Query(Criteria.where("id").is(fileId));
        if (isThoroughly) {
            DeleteResult result = mongoTemplate.remove(query, File.class);
            return result.getDeletedCount() > 0;
        } else {
            Update update = new Update();
            update.set("isDelete", true);
            update.set("isTrashbin", true);
            update.set("updatedDt", new Date());
            UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
            return result.getModifiedCount() > 0;
        }
    }

    @Override
    public boolean deleteFolder(String fileId, String oldFullPath, String newFullPath, boolean isThoroughly) {
        // 构建查询条件，匹配以 oldFullPath 开头的记录
        Document query = new Document("fullPath", new Document("$regex", "^" + oldFullPath));

        // 构建更新操作
        Document update = new Document("$set", new Document()
                .append("fullPath",
                        new Document("$concat", Arrays.asList(
                                newFullPath,  // 新前缀
                                new Document("$substrCP", Arrays.asList(
                                        "$fullPath",
                                        oldFullPath.length(),  // 截取点
                                        new Document("$subtract", Arrays.asList(
                                                new Document("$strLenCP", "$fullPath"),  // 总字符串长度
                                                oldFullPath.length()  // 减去旧前缀的长度
                                        ))
                                ))
                        ))
                )
                .append("isDelete", true)
//                .append("isRealDelete", isThoroughly)
                .append("updatedDt", new Date())); // 同时设置 updatedDt 字段

        // 执行原生 MongoDB 批量更新操作
        mongoTemplate.getDb().getCollection("file").updateMany(query, Arrays.asList(update));
        return true;
    }

    @Override
    public boolean deleteFolder(String fullPath, boolean isThoroughly) {
        // 注意：设置没被删除的数据所以isDelete=false
        Query query = new Query(new Criteria().and("isDelete").is(isThoroughly)
                .andOperator(
                        new Criteria().orOperator(
                                Criteria.where("isTrashbin").is(false),
                                Criteria.where("isTrashbin").exists(false)
                        )
                )
                .and("fullPath").regex("^" + Pattern.quote(fullPath) + ".*"));
        if (isThoroughly) {
            DeleteResult result = mongoTemplate.remove(query, File.class);
            return result.getDeletedCount() > 0;
        } else {
            Update update = new Update();
            update.set("isDelete", true);
            update.set("updatedDt", new Date());
            update.unset("isTrashbin");
            UpdateResult result = mongoTemplate.updateMulti(query, update, File.class);
            return result.getModifiedCount() > 0;
        }
    }

    /**
     * 彻底删除
     *
     * @param fileId
     * @return
     */
    @Override
    public boolean deleteThoroughly(String fileId) {
        Query query = new Query(Criteria.where("id").is(fileId).and("isDelete").is(false));
        Update update = new Update();
        update.set("isDelete", true);
//        update.set("isRealDelete", true);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateMulti(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean putInTrashbin(String fileId) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("isTrashbin", true);
        UpdateResult result = mongoTemplate.updateMulti(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean takeOutTrashbin(String fileId) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.unset("isTrashbin");
        UpdateResult result = mongoTemplate.updateMulti(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean recovery(String fileId) {
        return false;
    }

    @Override
    public boolean editContent(File file) {
        return true;
    }

    @Override
    public boolean editCategoryId(String fileId, String categoryId) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("categoryId", categoryId);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean move(String fileId, String pid, String fullPath) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        if (StringUtils.hasLength(pid)) {
            update.set("pid", pid);
        } else {
            update.unset("pid");
        }

        update.set("fullPath", fullPath);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean moveFolder(String fileId, String pid, String oldFullPath, String newFullPath) {
        log.info("oldFullPath={}", oldFullPath);
        log.info("newFullPath={}", newFullPath);
        // 构建查询条件，匹配以 oldFullPath 开头的记录
        Document query = new Document("fullPath", new Document("$regex", "^" + oldFullPath));

        // 构建更新操作
        Document update = new Document("$set", new Document()
                .append("fullPath",
                        new Document("$concat", Arrays.asList(
                                newFullPath,  // 新前缀
                                new Document("$substrCP", Arrays.asList(
                                        "$fullPath",
                                        oldFullPath.length(),  // 截取点
                                        new Document("$subtract", Arrays.asList(
                                                new Document("$strLenCP", "$fullPath"),  // 总字符串长度
                                                oldFullPath.length()  // 减去旧前缀的长度
                                        ))
                                ))
                        ))
                )
                .append("updatedDt", new Date())); // 同时设置 updatedDt 字段

        // 执行原生 MongoDB 批量更新操作
        mongoTemplate.getDb().getCollection("file").updateMany(query, Arrays.asList(update));
        return true;
    }

    @Override
    public boolean recovery(File file) {
        Query query = new Query(Criteria.where("id").is(file.getId()));
        Update update = new Update();
        update.set("isDelete", false);
//        update.set("isRealDelete", false);
        update.unset("isTrashbin");
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean recovery(File oldFile, File newFile) {
        if (newFile.getType().equals("folder")) {
            // 如果是文件夹批量修改fullPath
            Document query = new Document("fullPath", new Document("$regex", "^" + oldFile.getFullPath()))
                    .append("isDelete", true)
                    // 排除isTrashbin=true字段
                    .append("$or", Arrays.asList(
                            new Document("isTrashbin", false), // isTrashbin = false
                            new Document("isTrashbin", new Document("$exists", false)) // isTrashbin 不存在
                    ));

            // 构建更新操作
            Document update = new Document("$set", new Document()
                    .append("fullPath",
                            new Document("$concat", Arrays.asList(
                                    newFile.getFullPath(),  // 新前缀
                                    new Document("$substrCP", Arrays.asList(
                                            "$fullPath",
                                            oldFile.getFullPath().length(),  // 截取点
                                            new Document("$subtract", Arrays.asList(
                                                    new Document("$strLenCP", "$fullPath"),  // 总字符串长度
                                                    oldFile.getFullPath().length()  // 减去旧前缀的长度
                                            ))
                                    ))
                            ))
                    )
                    .append("isDelete", false)
//                    .append("isRealDelete", false)
                    .append("updatedDt", new Date())); // 同时设置 updatedDt 字段

            //修改文件夹的name和删除isTrashbin字段
            Query query2 = new Query(Criteria.where("id").is(oldFile.getId()));
            Update update2 = new Update();
            update2.set("name", newFile.getName());
            if (StringUtils.hasLength(newFile.getPid())) {
                update2.set("pid", newFile.getPid());
            } else {
                update2.unset("pid");
            }

            update2.set("updatedDt", new Date());
            // 删除isTrashbin字段
            update2.unset("isTrashbin");
            UpdateResult result = mongoTemplate.updateFirst(query2, update2, File.class);
            boolean isSuccess = result.getModifiedCount() > 0;
            // 执行原生 MongoDB 批量更新操作
            return mongoTemplate.getDb().getCollection("file").updateMany(query, Arrays.asList(update)).getModifiedCount() > 0 && isSuccess;
        } else {
            //如果是文件直接替换
            Query query = new Query(Criteria.where("id").is(oldFile.getId()).and("isDelete").is(true));
            Update update = new Update();
            update.set("name", newFile.getName());
            update.set("url", newFile.getUrl());
            update.set("path", newFile.getPath());
            update.set("fullPath", newFile.getFullPath());
            if (StringUtils.hasLength(newFile.getPid())) {
                update.set("pid", newFile.getPid());
            } else {
                update.unset("pid");
            }
            update.set("isDelete", false);
//            update.set("isRealDelete", false);
            // 删除isTrashbin字段
            update.unset("isTrashbin");
            update.set("updatedDt", new Date());
            UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
            return result.getModifiedCount() > 0;
        }
    }

    @Override
    public boolean editIsPrivate(String fileId, Boolean isPrivate) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("isPrivate", isPrivate);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editFileName(String fileId, String fileName, String fileUrl, String filePath) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("name", fileName);
        update.set("url", fileUrl);
        update.set("path", filePath);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editFileName(String fileId, String fileName, String fileUrl, String filePath, String fullPath) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("name", fileName);
        update.set("url", fileUrl);
        update.set("path", filePath);
        update.set("fullPath", fullPath);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean editFolderName(String fileId, String fileName, String oldFullPath, String newFullPath) {

        log.info("--------------fileName={}", fileName);
        log.info("--------------oldFullPath={}", oldFullPath);
        log.info("--------------newFullPath={}", newFullPath);


        //1、修改文件夹名称
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("name", fileName);
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);

        // 转义特殊字符
        String escapedOldFullPath = Pattern.quote(oldFullPath);
        // 打印转义结果用于调试
        log.info("转义后的正则表达式2: {}", escapedOldFullPath);

        //2、匹配到的文件夹的oldFullPath,批量替换成newFullPath
        // 构建查询条件，匹配以 oldFullPath 开头的记录
        // 注意：oldFullPath可能出现跟正则相同的字符串，所以要转义一下，否则修改不成功，如上面的+号！！！
        // 后面如果还发现修改文件夹不成功，可能就是这个原因导致的
        Document query2 = new Document("fullPath", new Document("$regex", "^" + escapedOldFullPath));

        // 构建更新操作
        Document update2 = new Document("$set", new Document()
                .append("fullPath",
                        new Document("$concat", Arrays.asList(
                                newFullPath,  // 新前缀
                                new Document("$substrCP", Arrays.asList(
                                        "$fullPath",
                                        oldFullPath.length(),  // 截取点
                                        new Document("$subtract", Arrays.asList(
                                                new Document("$strLenCP", "$fullPath"),  // 总字符串长度
                                                oldFullPath.length()  // 减去旧前缀的长度
                                        ))
                                ))
                        ))
                )
                .append("updatedDt", new Date())); // 同时设置 updatedDt 字段

        // 执行原生 MongoDB 批量更新操作
        mongoTemplate.getDb().getCollection("file").updateMany(query2, Arrays.asList(update2));

        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean setFavorite(String fileId, Boolean isFavorite) {
        log.info("setFavorite fileId={},isFavorite={}", fileId, isFavorite);
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("isFavorite", isFavorite);
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean updateDt(String fileId) {
        Query query = new Query(Criteria.where("id").is(fileId));
        Update update = new Update();
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, File.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public File queryFileById(String fileId) {
        Query query = new Query(Criteria.where("id").is(fileId).and("isDelete").is(false));
        return mongoTemplate.findOne(query, File.class);
    }

    @Override
    public File queryFileByFullPath(String uid, String fullPath) {
        Query query = new Query(Criteria.where("uid").is(uid).and("fullPath").is(fullPath).and("isDelete").is(false));
        return mongoTemplate.findOne(query, File.class);
    }

    /**
     * 注意：folderFullPath 是文件夹的全路径
     *
     * @param uid
     * @param folderFullPath
     * @param fileName
     * @return
     */
    @Override
    public boolean searchFolderPathIsExistFile(String uid, String folderPrevPath, String fileName, boolean isFolder) {
        log.info("searchFolderPathIsExistFile,folderPrevPath={},fileName={}", folderPrevPath, fileName);
        Query query;
        Criteria criteria;
        // 注意：如果是文件夹时，fullpath要在prevpath+filename基础上，后缀再加个"/"
        String fullPath = isFolder ? folderPrevPath + fileName + "/" : folderPrevPath + fileName;
        criteria = Criteria.where("uid").is(uid)
                .and("fullPath").is(fullPath)
                .and("name").is(fileName).and("isDelete").is(false);

        query = new Query(criteria);
        return mongoTemplate.findOne(query, File.class) != null;
    }

    @Override
    public boolean isExistFullPath(String uid, String fullPath) {
        Criteria criteria = Criteria.where("uid").is(uid).and("fullPath").is(fullPath).and("isDelete").is(false);
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query, File.class) != null;
    }

    @Override
    public boolean isExistShareFriend(String uid, String fileId, String friendId) {
        Criteria criteria = Criteria.where("uid").is(uid)
                .and("id").is(fileId)
                .and("isDelete").is(false)
                .and("shareToFriends.uid").is(friendId);
        Query query = new Query(criteria);
        return mongoTemplate.findOne(query, File.class) != null;
    }


    @Override
    public List<File> queryFileByPid(String pid) {
        Criteria criteria = Criteria.where("isDelete").is(false).and("type").ne("folder");
        // 查询根目录文件
        if (!StringUtils.hasLength(pid) || pid.equals("-1")) {
            criteria.and("pid").exists(false);
        } else {
            // 查询子文件夹文件
            criteria.and("pid").is(pid);
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFileByPid(String pid, String uid) {
        Criteria criteria = Criteria.where("isDelete").is(false).and("type").ne("folder");

        // 添加 uid 条件
        if (StringUtils.hasLength(uid)) {
            criteria.and("uid").is(uid);
        }

        // 查询根目录文件
        if (!StringUtils.hasLength(pid) || pid.equals("-1")) {
            criteria.and("pid").exists(false);
        } else {
            // 查询子文件夹文件
            criteria.and("pid").is(pid);
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }


    @Override
    public List<File> queryFileListByFullPath(String uid, String fullPath) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false)
                .and("fullPath").regex("^" + Pattern.quote(fullPath) + "[^/]+/?$"));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFileListByFullPath(String uid, String fullPath, Boolean isDelete) {
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(isDelete).and("fullPath").regex("^" + Pattern.quote(fullPath) + "[^/]+/?$"));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFileListByType(String uid, String type) {
        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false);

        // 根据类型添加文件名过滤条件
        switch (type.toLowerCase()) {
            case "image":
                criteria.and("name").regex(IMAGE_REGEX, "i").and("type").ne("folder");
                break;
            case "video":
                criteria.and("name").regex(VIDEO_REGEX, "i").and("type").ne("folder");
                break;
            case "audio":
                criteria.and("name").regex(AUDIO_REGEX, "i").and("type").ne("folder");
                break;
            case "document":
                criteria.and("name").regex(DOCUMENT_REGEX, "i").and("type").ne("folder");
                break;
            case "others":
                Criteria othersCriteria = new Criteria().andOperator(
                        Criteria.where("name").not().regex(IMAGE_REGEX, "i"),
                        Criteria.where("name").not().regex(VIDEO_REGEX, "i"),
                        Criteria.where("name").not().regex(AUDIO_REGEX, "i"),
                        Criteria.where("name").not().regex(DOCUMENT_REGEX, "i"),
                        Criteria.where("type").ne("folder")
                );
                criteria.andOperator(othersCriteria);
                break;
        }
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFileListByFolder(String uid) {
        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).and("type").is("folder");
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFileListByTwoWeeks(String uid, String fullPath) {
        // 获取两周前的日期
        LocalDate twoWeeksAgo = LocalDate.now().minusWeeks(2);
        Date twoWeeksAgoDate = Date.from(twoWeeksAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Query query = new Query(
                Criteria.where("uid").is(uid)
                        .and("isDelete").is(false)
                        .and("fullPath").regex("^" + Pattern.quote(fullPath) + "[^/]+/?$")
                        .and("updatedDt").gte(twoWeeksAgoDate) // 查询最近两周的数据
        );
        query.with(Sort.by(Sort.Order.desc("updatedDt")));

        return mongoTemplate.find(query, File.class);
    }

    /**
     * 查询所有节点是收藏的数据，并且父节点不能也是收藏
     *
     * @param uid
     * @return
     */
    @Override
    public List<File> queryFileListByFavorites(String uid) {
        // 构建聚合查询
        Aggregation aggregation = Aggregation.newAggregation(
                // 第一步：筛选出符合 uid 且 isFavorite = true 的文件
                Aggregation.match(
                        Criteria.where("uid").is(uid)
                                .and("isFavorite").is(true)
                                .and("isDelete").is(false)
                ),
                // 注意：如果pid是空字符串转换就失败，这个还没解决，看看后面有啥解决办法
                AddFieldsOperation.addField("pid").withValue(ConvertOperators.ToObjectId.toObjectId("$pid")).build(),

                // 第二步：通过 lookup 关联父节点信息，假设父节点和子节点都在 "file" 集合中
                Aggregation.lookup("file", "pid", "_id", "parentFiles"),

                // 第三步：使用 unwind 展开 parentFiles 数组，并允许没有父节点的情况
                Aggregation.unwind("parentFiles", true), // `true` 允许父节点为空

                // 第四步：根据有无父节点进行过滤
                Aggregation.match(
                        new Criteria().orOperator(
                                Criteria.where("pid").exists(false),                    // 没有父节点的文件
                                Criteria.where("parentFiles.isFavorite").is(false),     // 父节点 isFavorite = false
                                Criteria.where("parentFiles.isFavorite").exists(false)  // 父节点没有 isFavorite 字段
                        )
                ),

                // 第五步：按更新时间倒序排列
                Aggregation.sort(Sort.by(Sort.Order.desc("updatedDt")))
        );

        // 执行聚合查询并返回结果
        return mongoTemplate.aggregate(aggregation, "file", File.class).getMappedResults();
    }

    /**
     * 1、查询所有节点是收藏的数据，并且父节点不能也是收藏
     * 2、如果fullPath有值，并且不是/，则查询该fullpath下的所有子节点(类似我的文件查询)
     *
     * @param uid
     * @param fullPath
     * @return
     */
    @Override
    public List<File> queryFileListByFavorites(String uid, String fullPath) {
        // 创建基础条件
        Criteria criteria = Criteria.where("uid").is(uid)
                .and("isDelete").is(false);

        // 如果 fullPath 非空并且不是/，则加入正则表达式条件
        if (StringUtils.hasLength(fullPath) && !fullPath.equals("/")) {
            criteria = criteria.and("fullPath").regex("^" + Pattern.quote(fullPath) + "[^/]+/?$");
        } else {
            criteria = criteria.and("isFavorite").is(true);

        }

        // 构建聚合查询
        List<AggregationOperation> operations = new ArrayList<>();

        // 第一步：筛选出符合 uid, isFavorite = true, isDelete = false 的文件
        operations.add(Aggregation.match(criteria));

        // 如果没有 fullPath或者fullPath=/，则添加以下操作
        if (!StringUtils.hasLength(fullPath) || fullPath.equals("/")) {
            // 第二步：将 pid 转换为 ObjectId
            operations.add(AddFieldsOperation.addField("pid").withValue(ConvertOperators.ToObjectId.toObjectId("$pid")).build());

            // 第三步：通过 lookup 关联父节点信息
            operations.add(Aggregation.lookup("file", "pid", "_id", "parentFiles"));

            // 第四步：使用 unwind 展开 parentFiles 数组，并允许没有父节点的情况
            operations.add(Aggregation.unwind("parentFiles", true)); // `true` 允许父节点为空

            // 第五步：根据有无父节点进行过滤
            operations.add(Aggregation.match(
                    new Criteria().orOperator(
                            Criteria.where("pid").exists(false),                    // 没有父节点的文件
                            Criteria.where("parentFiles.isFavorite").is(false),     // 父节点 isFavorite = false
                            Criteria.where("parentFiles.isFavorite").exists(false)  // 父节点没有 isFavorite 字段
                    )
            ));
        }

        // 第六步：按更新时间倒序排列
        operations.add(Aggregation.sort(Sort.by(Sort.Order.desc("updatedDt"))));

        // 构建最终的聚合查询
        Aggregation aggregation = Aggregation.newAggregation(operations);

        return mongoTemplate.aggregate(aggregation, "file", File.class).getMappedResults();
    }

    @Override
    public List<File> queryDeleteFileListByOneMonth(String uid) {
        // 获取两周前的日期
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());
//
        Query query = new Query(Criteria.where("uid").is(uid)
//                .and("belongTo").exists(false)
                .and("isDelete").is(true)
                .and("isTrashbin").is(true)
//                .andOperator(
//                        new Criteria().orOperator(
//                                Criteria.where("isRealDelete").is(false),   // 存在并且为 false
//                                Criteria.where("isRealDelete").exists(false) // 字段不存在
//                        )
//                )
                .and("updatedDt").gte(oneMonthAgoDate));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);


        // 构建聚合查询
//        Aggregation aggregation = Aggregation.newAggregation(
//                // 第一步：筛选出符合 uid 且 isFavorite = true 的文件
//                Aggregation.match(
//                        Criteria.where("uid").is(uid)
//                                .and("isDelete").is(true)
//                                .and("isTrashbin").is(true)
////                                .andOperator(
////                                        new Criteria().orOperator(
////                                                Criteria.where("isRealDelete").is(false),   // 存在并且为 false
////                                                Criteria.where("isRealDelete").exists(false) // 字段不存在
////                                        )
////                                )
//                                .and("updatedDt").gte(oneMonthAgoDate)
//                ),
//                AddFieldsOperation.addField("pid").withValue(ConvertOperators.ToObjectId.toObjectId("$pid")).build(),
//
//                // 第二步：通过 lookup 关联父节点信息，假设父节点和子节点都在 "file" 集合中
//                Aggregation.lookup("file", "pid", "_id", "parentFiles"),
//
//                // 第三步：使用 unwind 展开 parentFiles 数组，并允许没有父节点的情况
//                Aggregation.unwind("parentFiles", true), // `true` 允许父节点为空
//
//                // 第四步：根据有无父节点进行过滤
//                Aggregation.match(
//                        new Criteria().orOperator(
//                                Criteria.where("pid").exists(false),                    // 没有父节点的文件
//                                Criteria.where("parentFiles.isDelete").is(false),     // 父节点 isFavorite = false
//                                Criteria.where("parentFiles.isDelete").exists(false)  // 父节点没有 isFavorite 字段
//                        )
//                ),
//
//                // 第五步：按更新时间倒序排列
//                Aggregation.sort(Sort.by(Sort.Order.desc("updatedDt")))
//        );
//
//        // 执行聚合查询并返回结果
//        return mongoTemplate.aggregate(aggregation, "file", File.class).getMappedResults();
    }

    @Override
    public List<File> queryDeleteFileListByOneMonth(String uid, String fullPath) {
        // 获取两周前的日期
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        Date oneMonthAgoDate = Date.from(oneMonthAgo.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Query query = new Query(
                Criteria.where("uid").is(uid)
                        .and("isDelete").is(true)
//                        .andOperator(
//                                new Criteria().orOperator(
//                                        Criteria.where("isRealDelete").is(false),   // 存在并且为 false
//                                        Criteria.where("isRealDelete").exists(false) // 字段不存在
//                                )
//                        )
                        .and("fullPath").regex("^" + Pattern.quote(fullPath) + "/[^/]+$")
                        .and("updatedDt").gte(oneMonthAgoDate) // 查询最近两周的数据
        );
        query.with(Sort.by(Sort.Order.desc("updatedDt")));

        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFileByTitle(String uid, String title) {
        // 模糊匹配
        Pattern pattern = Pattern.compile("^.*" + Pattern.quote(title) + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = Criteria.where("uid").is(uid).and("title").regex(pattern).and("isDelete").is(false);
        Criteria criteria2 = Criteria.where("uid").is(uid).and("domain").regex(pattern).and("isDelete").is(false);
        Query query = new Query(new Criteria().orOperator(criteria1, criteria2));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public boolean findFileAndDelete(String uid, String fileName) {
        Query query = new Query(Criteria.where("uid").is(uid).and("name").is(fileName));
        DeleteResult result = mongoTemplate.remove(query, File.class);
        return result.getDeletedCount() > 0;
    }

    /**
     * 获取size 求和
     *
     * @param uid
     * @return
     */
    @Override
    public File getFileSizeByUserId(String uid) {
        MatchOperation matchOperation = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false));
        // 注意，后面的size必须是File类的属性，这样下面的results.getUniqueMappedResult().getSize();才能获取到值，就是说把统计求和的值放到size属性中
        GroupOperation groupOperation = Aggregation.group("uid").sum("size").as("size");
        Aggregation agg = Aggregation.newAggregation(matchOperation, groupOperation);
        //查询、并获取结果
        AggregationResults<File> results = mongoTemplate.aggregate(agg, "file", File.class);
        return results.getUniqueMappedResult();
    }

    @Override
    public List<File> queryFolderList(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("type").is("folder").and("isDelete").is(false));
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryFolderList(String uid, List<String> fileIds) {
        Criteria criteria = Criteria.where("uid").is(uid).and("type").is("folder")
                .and("isDelete").is(false).and("id").nin(fileIds);
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("updatedDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryMyShareFileList(String uid) {
        Criteria criteria = Criteria.where("uid").is(uid)
                .and("isDelete").is(false)
                .andOperator(
                        Criteria.where("shareToFriends").exists(true),  // 确保字段存在
                        Criteria.where("shareToFriends").ne(Collections.emptyList())  // 确保字段不为空
                );
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("shareToFriends.0.shareDt")));
        return mongoTemplate.find(query, File.class);
    }

    @Override
    public List<File> queryShareFileToMe(String myUid) {
        Criteria criteria = Criteria.where("isDelete").is(false)
                .and("shareToFriends").elemMatch(
                        Criteria.where("uid").is(myUid)
                );
        Query query = new Query(criteria);
        query.with(Sort.by(Sort.Order.desc("shareToFriends.shareDt")));

        return mongoTemplate.find(query, File.class);
    }
}
