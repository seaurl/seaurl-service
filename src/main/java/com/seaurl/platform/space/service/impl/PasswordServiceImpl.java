package com.seaurl.platform.space.service.impl;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.seaurl.platform.space.model.PasswordCategory;
import com.seaurl.platform.space.model.PasswordEntry;
import com.seaurl.platform.space.service.PasswordService;
import com.seaurl.platform.space.utils.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Service
public class PasswordServiceImpl implements PasswordService {

    private static Logger log = LoggerFactory.getLogger(PasswordServiceImpl.class);

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<PasswordEntry> queryAll(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedDt"));
        List<PasswordEntry> list = mongoTemplate.find(query, PasswordEntry.class);
        decryptAll(list, uid);
        return list;
    }

    @Override
    public List<PasswordEntry> search(String uid, String keyword) {
        Query query = new Query(Criteria.where("uid").is(uid));
        if (StringUtils.hasLength(keyword)) {
            Criteria titleMatch = Criteria.where("title").regex(keyword, "i");
            Criteria userMatch = Criteria.where("username").regex(keyword, "i");
            Criteria urlMatch = Criteria.where("url").regex(keyword, "i");
            query.addCriteria(new Criteria().orOperator(titleMatch, userMatch, urlMatch));
        }
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "updatedDt"));
        query.limit(200);
        List<PasswordEntry> list = mongoTemplate.find(query, PasswordEntry.class);
        decryptAll(list, uid);
        return list;
    }

    @Override
    public PasswordEntry add(String uid, String title, String username, String password, String url, String repoUrl, String repoBranch, String notes, String category, String parentId, String color, String categoryId) {
        Date date = new Date();
        PasswordEntry entry = new PasswordEntry();
        entry.setUid(uid);
        entry.setTitle(title);
        entry.setUsername(username);
        entry.setUrl(url != null ? url : "");
        entry.setRepoUrl(repoUrl != null ? repoUrl : "");
        entry.setRepoBranch(repoBranch != null ? repoBranch : "");
        entry.setNotes(notes != null ? notes : "");
        entry.setCategory(category != null ? category : "");
        entry.setCategoryId(StringUtils.hasLength(categoryId) ? categoryId : null);
        entry.setParentId(parentId != null ? parentId : "");
        entry.setColor(color != null ? color : "#52c41a");
        entry.setCreatedDt(date);
        entry.setUpdatedDt(date);

        if (StringUtils.hasLength(password)) {
            String encrypted = PasswordUtil.encrypt(password, uid);
            String[] parts = encrypted.split(":");
            entry.setPasswordEncrypted(parts[0]);
            entry.setIv(parts[1]);
            entry.setAuthTag(parts[2]);
        }

        return mongoTemplate.insert(entry);
    }

    @Override
    public boolean update(String id, String title, String username, String password, String url, String repoUrl, String repoBranch, String notes, String category, String parentId, String color, String categoryId) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update();
        if (StringUtils.hasLength(title)) update.set("title", title);
        if (StringUtils.hasLength(username)) update.set("username", username);
        if (url != null) update.set("url", url);
        if (repoUrl != null) update.set("repoUrl", repoUrl);
        if (repoBranch != null) update.set("repoBranch", repoBranch);
        if (notes != null) update.set("notes", notes);
        if (category != null) update.set("category", category);
        if (parentId != null) update.set("parentId", parentId);
        if (color != null) update.set("color", color);
        if (categoryId != null) update.set("categoryId", categoryId);
        update.set("updatedDt", new Date());

        if (StringUtils.hasLength(password)) {
            PasswordEntry existing = mongoTemplate.findOne(query, PasswordEntry.class);
            if (existing != null) {
                String encrypted = PasswordUtil.encrypt(password, existing.getUid());
                String[] parts = encrypted.split(":");
                update.set("passwordEncrypted", parts[0]);
                update.set("iv", parts[1]);
                update.set("authTag", parts[2]);
            }
        }

        UpdateResult result = mongoTemplate.updateFirst(query, update, PasswordEntry.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean delete(String id) {
        Query childQuery = new Query(Criteria.where("parentId").is(id));
        mongoTemplate.remove(childQuery, PasswordEntry.class);
        Query query = new Query(Criteria.where("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, PasswordEntry.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public List<PasswordCategory> categories(String uid) {
        Query query = new Query(Criteria.where("uid").is(uid));
        return mongoTemplate.find(query, PasswordCategory.class);
    }

    @Override
    public PasswordCategory addCategory(String uid, String name, String parentId) {
        Date date = new Date();
        PasswordCategory cat = new PasswordCategory();
        cat.setUid(uid);
        cat.setName(name.trim());
        cat.setParentId(StringUtils.hasLength(parentId) ? parentId.trim() : null);
        cat.setCreatedDt(date);
        cat.setUpdatedDt(date);
        return mongoTemplate.insert(cat);
    }

    @Override
    public boolean renameCategory(String uid, String id, String name) {
        // 账号靠 categoryId 关联，重命名分类无需同步账号
        Query query = new Query(Criteria.where("id").is(id).and("uid").is(uid));
        Update update = new Update();
        update.set("name", name.trim());
        update.set("updatedDt", new Date());
        UpdateResult result = mongoTemplate.updateFirst(query, update, PasswordCategory.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public List<PasswordCategory> queryCategoryByPid(String uid, String pid) {
        Query query = new Query(Criteria.where("uid").is(uid).and("parentId").is(pid));
        return mongoTemplate.find(query, PasswordCategory.class);
    }

    @Override
    public List<PasswordEntry> queryEntryByCategoryId(String uid, String categoryId) {
        Query query = new Query(Criteria.where("uid").is(uid).and("categoryId").is(categoryId));
        return mongoTemplate.find(query, PasswordEntry.class);
    }

    @Override
    public boolean deleteCategory(String id) {
        Query query = new Query(Criteria.where("id").is(id));
        DeleteResult result = mongoTemplate.remove(query, PasswordCategory.class);
        return result.getDeletedCount() > 0;
    }

    @Override
    public String moveCategory(String uid, String id, String parentId) {
        String target = StringUtils.hasLength(parentId) ? parentId : null;
        if (target != null) {
            // 不能移到自身
            if (id.equals(target)) {
                return "不能移动到自身下";
            }
            // 防环：目标不能是自身的子孙
            if (isDescendant(uid, id, target)) {
                return "不能移动到自身或其子分类下";
            }
            // 目标分类必须存在
            Query exist = new Query(Criteria.where("id").is(target).and("uid").is(uid));
            if (mongoTemplate.findOne(exist, PasswordCategory.class) == null) {
                return "目标分类不存在";
            }
        }
        Query query = new Query(Criteria.where("id").is(id).and("uid").is(uid));
        Update update = new Update();
        update.set("parentId", target);
        update.set("updatedDt", new Date());
        mongoTemplate.updateFirst(query, update, PasswordCategory.class);
        return null;
    }

    /**
     * 判断 candidateId 是否为 ancestorId 的（多级）子孙，用于移动防环
     */
    private boolean isDescendant(String uid, String ancestorId, String candidateId) {
        Queue<String> queue = new LinkedList<>();
        queue.add(ancestorId);
        Set<String> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (!visited.add(cur)) {
                continue;
            }
            Query q = new Query(Criteria.where("uid").is(uid).and("parentId").is(cur));
            for (PasswordCategory child : mongoTemplate.find(q, PasswordCategory.class)) {
                if (child.getId().equals(candidateId)) {
                    return true;
                }
                queue.add(child.getId());
            }
        }
        return false;
    }

    @Override
    public int migrate() {
        // 把老数据 entry.category(名字) 解析回填为 categoryId。幂等：已回填的跳过。
        List<PasswordEntry> entries = mongoTemplate.findAll(PasswordEntry.class);
        Map<String, String> cache = new HashMap<>(); // (uid::name) -> categoryId
        int migrated = 0;
        for (PasswordEntry entry : entries) {
            if (StringUtils.hasLength(entry.getCategoryId())) {
                continue;
            }
            String oldName = entry.getCategory() != null ? entry.getCategory().trim() : "";
            if (oldName.isEmpty()) {
                continue;
            }
            String key = entry.getUid() + "::" + oldName;
            String catId = cache.get(key);
            if (catId == null) {
                Query catQuery = new Query(Criteria.where("uid").is(entry.getUid()).and("name").is(oldName));
                PasswordCategory cat = mongoTemplate.findOne(catQuery, PasswordCategory.class);
                if (cat == null) {
                    cat = new PasswordCategory();
                    cat.setUid(entry.getUid());
                    cat.setName(oldName);
                    cat.setParentId(null);
                    Date date = new Date();
                    cat.setCreatedDt(date);
                    cat.setUpdatedDt(date);
                    cat = mongoTemplate.insert(cat);
                }
                catId = cat.getId();
                cache.put(key, catId);
            }
            Query updateQuery = new Query(Criteria.where("id").is(entry.getId()));
            Update update = new Update();
            update.set("categoryId", catId);
            update.set("updatedDt", new Date());
            mongoTemplate.updateFirst(updateQuery, update, PasswordEntry.class);
            migrated++;
        }
        return migrated;
    }

    private void decryptAll(List<PasswordEntry> list, String uid) {
        for (PasswordEntry entry : list) {
            if (StringUtils.hasLength(entry.getPasswordEncrypted())
                    && StringUtils.hasLength(entry.getIv())
                    && StringUtils.hasLength(entry.getAuthTag())) {
                try {
                    String combined = entry.getPasswordEncrypted() + ":" + entry.getIv() + ":" + entry.getAuthTag();
                    String plain = PasswordUtil.decrypt(combined, uid);
                    entry.setPasswordEncrypted(plain);
                } catch (Exception e) {
                    log.error("decrypt password failed for entry={}, error={}", entry.getId(), e.getMessage());
                }
            }
        }
    }
}
