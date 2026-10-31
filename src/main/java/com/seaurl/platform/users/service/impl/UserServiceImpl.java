package com.seaurl.platform.users.service.impl;

import java.util.Arrays;
import com.seaurl.platform.users.model.*;
import com.seaurl.platform.common.model.*;
import com.seaurl.platform.space.model.MailMessage;
import com.seaurl.platform.users.service.LevelService;
import com.seaurl.platform.users.service.UserService;
import com.seaurl.platform.users.utils.DateUtils;
import com.seaurl.platform.users.utils.DatesUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;
import static org.springframework.data.mongodb.core.aggregation.ArrayOperators.Filter.filter;

@Service("usersUserServiceImpl")
public class UserServiceImpl implements UserService {
    private static Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    private MongoOperations mongoOps;

    @Autowired
    LevelService levelService;

    @Override
    public boolean isExistUsername(String username) {
        Query query = new Query(Criteria.where("userName").is(username));
        User user = mongoTemplate.findOne(query, User.class);
        return user != null;
    }

    @Override
    public boolean isExistEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        User user = mongoTemplate.findOne(query, User.class);
        return user != null;
    }

    @Override
    public boolean isExistEmailAndIsVerified(String email) {
        Query query = new Query(Criteria.where("email").is(email).and("isVerified").is(true));
        UserAuth user = mongoTemplate.findOne(query, UserAuth.class);
        return user != null;
    }

    @Override
    public boolean updateUserVerified(String username) {
        Query query = Query.query(Criteria.where("userName").is(username));
        Update update = Update.update("isVerified", true);
        long count = mongoTemplate.updateFirst(query, update, UserAuth.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public boolean updateUserAvatar(String userName, String avatar) {
        Query query = Query.query(Criteria.where("userName").is(userName));
        Update update = Update.update("avatar", avatar);
        long count = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count > 0;
    }

    @Override
    public List<User> findAll() {
        List<User> users = mongoTemplate.find(new Query(new Criteria()), User.class);
        return users;
    }

    @Override
    public boolean isVerified(String username) {
        Query query = new Query(Criteria.where("userName").is(username));
        UserAuth user = mongoTemplate.findOne(query, UserAuth.class);

        return user.isVerified();
    }

    @Override
    public User getUserByName(String username) {
        Query query = new Query(Criteria.where("userName").is(username).and("status").is(2));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public List<User> getUserByName(List<String> excludeUids, String userName, Integer skipCount) {
        Pattern pattern = Pattern.compile(userName, Pattern.CASE_INSENSITIVE); // 可选：添加 CASE_INSENSITIVE 以实现不区分大小写的匹配
        Query query = new Query(Criteria.where("userName").regex(pattern).and("status").is(2).and("uid").nin(excludeUids));
        // 设置分页信息，这里查询前skipCount条数据
        query.limit(skipCount);
        return mongoTemplate.find(query, User.class);
    }

    @Override
    public User getUserByEmail(String email) {
        Query query = new Query(Criteria.where("email").is(email));
        return mongoTemplate.findOne(query, User.class);
    }

    @Override
    public Map<String, Object> getUserData(String uid) {
        Query query1 = Query.query(Criteria.where("uid").is(uid).and("isDelete").is(false));
//        Query query2 = Query.query(Criteria.where("uid").is(uid).and("isAttention").is(true));
        Long count1 = mongoTemplate.count(query1, Url.class);
//        Integer count2 = getLikeMeUrlCount(uid);
        Long count2 = getCopyUrlCount(uid);

        Query query3 = Query.query(Criteria.where("uid").is(uid));
        UserPoints userPoints = mongoTemplate.findOne(query3, UserPoints.class);
        Integer count3 = 0;
        if (userPoints != null){
            count3 = userPoints.getPoints();
        }

        Level level = levelService.getLevelByUid(uid);
        int count4 = 1;
        if (level != null){
            count4 = level.getLevel();
        }

        Query queryNote = Query.query(Criteria.where("uid").is(uid).and("isDelete").is(false));
        Query queryFile = Query.query(Criteria.where("uid").is(uid).and("isDelete").is(false));
        Long noteCount = mongoTemplate.count(queryNote, Note.class);
        Long fileCount = mongoTemplate.count(queryFile, File.class);

        Query queryMail = Query.query(Criteria.where("uid").is(uid));
        Query queryMailUnread = Query.query(Criteria.where("uid").is(uid).and("isUnread").is(true));
        Long mailCount = mongoTemplate.count(queryMail, MailMessage.class);
        Long mailUnreadCount = mongoTemplate.count(queryMailUnread, MailMessage.class);

        Query query5 = Query.query(Criteria.where("uid").is(uid).and("status").is(true));
        Query query6 = Query.query(Criteria.where("followedId").is(uid).and("status").is(true));
        Long count5 = mongoTemplate.count(query5, UserFollow.class);
        Long count6 = mongoTemplate.count(query6, UserFollow.class);

        Date start = DatesUtil.getDayBegin();
        Date end = DatesUtil.getDayEnd();
        Query query7 = Query.query(Criteria.where("watchUid").is(uid).and("createdDt").gt(start).lt(end));
        Query query8 = Query.query(Criteria.where("watchUid").is(uid));
        Integer count7 = mongoTemplate.find(query7, UserWatch.class).size();
        Integer count8 = mongoTemplate.find(query8, UserWatch.class).size();

        HashMap<String, Object> userData = new HashMap<>();

        // 获取积分和等级
        userData.put("level", count4);
        userData.put("points", count3);


        //获取网址数和复制网址数
        userData.put("urlCount", count1);
        userData.put("copyUrlCount", count2);
//        userData.put("likeMeUrlCount", count2);
//        userData.put("lastAddUrlCount", getRecentlyAddUrlsCount(uid));

        // 获取笔记和文件数
        userData.put("noteCount", noteCount);
        userData.put("fileCount", fileCount);

        // 获取邮件数和未读数
        userData.put("mailCount", mailCount);
        userData.put("mailUnreadCount", mailUnreadCount);

        // 获取关注和粉丝数
        userData.put("followingCount", count5);
        userData.put("followerCount", count6);

        // 获取今天和总浏览数
        userData.put("todayWatchMeCount", count7);
        userData.put("totalWatchMeCount", count8);
        return userData;
    }

    public Long getCopyUrlCount(String uid){
        Query query = new Query(Criteria.where("uid").is(uid).and("isDelete").is(false).and("isCopy").is(true));
        return mongoTemplate.count(query, Url.class);
    }

    /**
     * 获取别人喜欢我的网址的数量，也就是获赞数
     * @param uid
     * @return
     */
    public Integer getLikeMeUrlCount(String uid) {
        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("_id").withValue(ConvertOperators.ToString.toString("$_id")).build();
        LookupOperation lookupOperation = Aggregation.lookup("userLikeUrl", "_id", "urlId", "userLikeUrl_as");

        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("isDelete").is(false)
                // 判断like是否有效
                .and("userLikeUrl_as.valid").is(1)
                // 不能把自己点赞自己的数据也包含进去，否则就没意义了
                .and("userLikeUrl_as.uid").ne(uid));

        ProjectionOperation project = Aggregation.project("uid");

        UnwindOperation unwindOperation = Aggregation.unwind("userLikeUrl_as");

        List<AggregationOperation> operationList = Arrays.asList();
        operationList.add(addFieldsOperation);
        operationList.add(lookupOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(unwindOperation);
        operationList.add(project);
        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "url", HashMap.class);
        return aggregationResults.getMappedResults().size();
    }

    /**
     * 获取用户最近一个月添加网址数量
     * @param uid
     * @return
     */
    private Long getRecentlyAddUrlsCount(String uid) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar c = Calendar.getInstance();

        // 获取上个月数据
        c.setTime(new Date());
        c.add(Calendar.MONTH, -1);
        Date m = c.getTime();
        String mon = format.format(m);

        Criteria criteria = Criteria.where("uid").is(uid).and("isDelete").is(false).andOperator(Criteria.where("createdDt").gte(
                // 要使用ISO日期格式的才行，否则 时间会多少算8小时
                DateUtils.dateToISODate(mon)
        ));
        Long total = mongoTemplate.count(new Query(criteria), Url.class);
        return total;
    }

    /**
     * 获取我关注的人的uid列表
     *
     * @param uid
     * @return
     */
    @Override
    public List<String> getFollowingUserIdList(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("status").is(true));
        List<UserFollow> userFollows = mongoTemplate.find(query, UserFollow.class);
        List<String> uids = new ArrayList<>();
        userFollows.forEach(item -> {
            uids.add(item.getFollowedId());
        });

        return uids;
    }

    /**
     * 我关注的
     *
     * @param uid
     * @return
     */
    @Override
    public List<HashMap> getFollowingList(String uid, Long pageNum, Long pageSize) {
        List<AggregationOperation> operationList = Arrays.asList();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("followedId").withValue(ConvertOperators.ToObjectId.toObjectId("$followedId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        // from:关联从表名,localField:主表中的关联字段,foreignField:从表关联的字段,as:别名
        LookupOperation lookupOperation = Aggregation.lookup("user", "followedId", "uid", "user_as");
        MatchOperation match = Aggregation.match(Criteria.where("uid").is(uid).and("user_as.status").is(2).and("status").is(true));
        ProjectionOperation project = Aggregation.project("user_as.uid", "user_as.userName", "user_as.nickName", "user_as.signature", "user_as.introduction",
                "user_as.email", "user_as.avatar", "user_as.company", "user_as.school", "user_as.location").andInclude("updatedDt");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");
        FacetOperation facet = Aggregation.facet(
                count().as("count")).as("total")
                .and(skip(pageSize * (pageNum - 1)), limit(pageSize)).as("rows");
        UnwindOperation unwindOperation = Aggregation.unwind("user_as");
        operationList.add(lookupOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(unwindOperation);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(facet);
        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userFollow", HashMap.class);
        return aggregationResults.getMappedResults();
    }

    /**
     * 关注我的
     *
     * @param uid
     * @return
     */
    @Override
    public List<HashMap> getFollowerList(String uid, Long pageNum, Long pageSize) {
        List<AggregationOperation> operationList = Arrays.asList();
        // 这一步很重要，必须将主表的数据类型保持跟子表的一致，否则失效，也就是说：userClickUrl的urlId类型要保持跟url的_id保持一致
//        AddFieldsOperation addFieldsOperation = AddFieldsOperation.addField("followedId").withValue(ConvertOperators.ToObjectId.toObjectId("$followedId")).build();
        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        // from:关联从表名,localField:主表中的关联字段,foreignField:从表关联的字段,as:别名
        LookupOperation lookupOperation = Aggregation.lookup("user", "uid", "uid", "user_as");
        MatchOperation match = Aggregation.match(Criteria.where("followedId").is(uid).and("user_as.status").is(2).and("status").is(true));
        ProjectionOperation project = Aggregation.project("user_as.uid", "user_as.userName", "user_as.nickName", "user_as.signature", "user_as.introduction",
                "user_as.email", "user_as.avatar", "user_as.company", "user_as.school", "user_as.location").andInclude("updatedDt");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "updatedDt");
        FacetOperation facet = Aggregation.facet(
                count().as("count")).as("total")
                .and(skip(pageSize * (pageNum - 1)), limit(pageSize)).as("rows");
        UnwindOperation unwindOperation = Aggregation.unwind("user_as");
        operationList.add(lookupOperation);
        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(match);
        operationList.add(project);
        operationList.add(sort);
        operationList.add(facet);

        Aggregation agg = Aggregation.newAggregation(operationList);
        // collectionName:主表
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "userFollow", HashMap.class);
        return aggregationResults.getMappedResults();
    }

    @Override
    public List<HashMap> getRecommendedAttentionUsers(String uid) {
        return null;
    }

    @Override
    public boolean updateUserBackgroundImg(String uid, String imgSrc) {
        Query query = Query.query(Criteria.where("uid").is(uid));
        Update update = Update.update("backgroundImg", imgSrc);
        long count = mongoTemplate.updateFirst(query, update, User.class).getModifiedCount();
        return count > 0;
    }

    /**
     *
     * @return
     */
    @Override
    public List<HashMap> getTop10UsersInMonth(List<String> hadFollowedUids) {
        List<AggregationOperation> operationList = Arrays.asList();

        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "uid", "uid", "url_as");
//        // 当前时间
//        String currentDate = XDateUtils.getCurrentDate();

//        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        Calendar c = Calendar.getInstance();
//        // 获取前7天数据
//        c.setTime(new Date());
//        c.add(Calendar.MONTH, - 1);
//        Date m = c.getTime();
//        String prevMonth = format.format(m);

        // 用户网址数排序并且不是我关注的用户
        AggregationOperation match = Aggregation.match(Criteria.where("uid").nin(hadFollowedUids).and("status").is(2).and("urlCount").gt(0));
        ProjectionOperation project = Aggregation.project("uid", "status", "userName", "avatar", "createdDt")
                .and(filter("url_as").as("item")
                        .by(ComparisonOperators.Eq.valueOf("item.isDelete").equalToValue(false))).as("url_as_list");

        ProjectionOperation project2 = Aggregation.project("uid", "status", "userName", "avatar", "createdDt")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
                .andExpression("{$size: '$url_as_list'}").as("urlCount");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "urlCount");
        LimitOperation limit = Aggregation.limit(10);
        operationList.add(lookupOperation);
//        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(project);
        operationList.add(project2);
        operationList.add(match);
        operationList.add(sort);
        operationList.add(limit);
        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg, "user", HashMap.class);
        List<HashMap> dataList = aggregationResults.getMappedResults();
        return dataList;
    }

    @Override
    public PageResults getTopUsersByPage(List<String> hadFollowedUids, Integer pageNum, Integer pageSize) {
        log.info("==================================查询未关注用户列表={}", hadFollowedUids);
        List<AggregationOperation> operationList = Arrays.asList();

        // url_as是子表别名，后面的操作涉及到子表的时候会用到
        LookupOperation lookupOperation = Aggregation.lookup("url", "uid", "uid", "url_as");

        // 用户网址数排序并且不是我关注的用户
        AggregationOperation match = Aggregation.match(Criteria.where("uid").nin(hadFollowedUids).and("status").is(2).and("urlCount").gt(0));

        ProjectionOperation project = Aggregation.project("uid", "status", "userName", "avatar", "createdDt")
                .and(filter("url_as").as("item")
                        .by(ComparisonOperators.Eq.valueOf("item.isDelete").equalToValue(false))).as("url_as_list");

        ProjectionOperation project2 = Aggregation.project("uid", "status", "userName", "avatar", "createdDt")
                // 再对userLikeUrl_as_list统计数量到userLikeUrl_count
                .andExpression("{$size: '$url_as_list'}").as("urlCount");
        // sort里面的clickCount字段必须从Project中取，否则 失败
        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "urlCount");

        AggregationOperation skip = skip((pageNum - 1) * pageSize);
        AggregationOperation limit = limit(pageSize);

//        LimitOperation limit = Aggregation.limit(10);
        operationList.add(lookupOperation);
//        operationList.add(unwindOperation);
        //因为match里面含有url_as，所以lookupOperation要在match上面
        operationList.add(project);
        operationList.add(project2);
        operationList.add(match);
        operationList.add(sort);

        Aggregation agg = Aggregation.newAggregation(operationList);
        AggregationResults<Document> totalResult = mongoTemplate.aggregate(agg, "user", Document.class);
        // 计算总页数
        long totalCount = totalResult.getMappedResults().size();


        operationList.add(skip);
        operationList.add(limit);

        Aggregation agg2 = Aggregation.newAggregation(operationList);
        AggregationResults<HashMap> aggregationResults = mongoTemplate.aggregate(agg2, "user", HashMap.class);

        List<HashMap> results = aggregationResults.getMappedResults();

        PageResults queryResults = new PageResults();
        queryResults.setPageNum(pageNum);
        queryResults.setPageSize(pageSize);
        queryResults.setTotal(totalCount);
        queryResults.setRows(results);
        return queryResults;
    }
}
