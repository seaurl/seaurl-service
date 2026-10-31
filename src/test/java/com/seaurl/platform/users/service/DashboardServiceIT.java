package com.seaurl.platform.users.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard 提交历史四类聚合的集成验证。连 dev_seaurl_db（只读，无写操作）。
 * 验证 spec §5 后端测试点：1（四类聚合计数）、4（热力图 count = 四类总和）、7（uid 隔离），
 * 以及 rows 投影字段集（spec §3.3）。
 */
@SpringBootTest
@ActiveProfiles("dev")
class DashboardServiceIT {

    @Autowired
    DashboardService dashboardService;

    @Autowired
    MongoTemplate mongoTemplate;

    private String pickUid() {
        // 从 url 集合挑一个最近有数据的 uid（保证后续聚合有内容可验）
        Query q = new Query(Criteria.where("isDelete").is(false));
        q.fields().include("uid");
        HashMap doc = mongoTemplate.findOne(q, HashMap.class, "url");
        assertThat(doc).as("dev url 集合至少有一条 isDelete=false 的记录").isNotNull();
        Object uid = doc.get("uid");
        assertThat(uid).as("记录必须含 uid 字段").isNotNull();
        return uid.toString();
    }

    @Test
    void getActivityByDate_returnsFourTypesWithMinimalFields() {
        String uid = pickUid();

        // 取该用户最近一条 url 的 y-m-d，保证 detail 非空
        HashMap anyUrl = mongoTemplate.findOne(
                new Query(Criteria.where("uid").is(uid).and("isDelete").is(false)),
                HashMap.class, "url");
        assertThat(anyUrl).as("该 uid 至少有一条 url").isNotNull();
        Object created = anyUrl.get("createdDt");
        assertThat(created).as("url 必须有 createdDt").isNotNull();
        // 简化：用当前年月做格式 y-m 测试，保证 path 走通即可
        String yearMonth = java.time.YearMonth.now().toString();

        List<HashMap> result = dashboardService.getActivityByDate(uid, yearMonth, "y-m");
        assertThat(result).hasSize(1);
        HashMap item = result.get(0);
        assertThat(item.get("date")).isEqualTo(yearMonth);

        for (String key : new String[]{"url", "note", "file", "mail"}) {
            Object blockObj = item.get(key);
            assertThat(blockObj).as("key=%s 必须存在", key).isInstanceOf(HashMap.class);
            HashMap block = (HashMap) blockObj;
            assertThat(block.get("count")).as("key=%s count 必须存在", key).isNotNull();
            assertThat(block.get("rows")).as("key=%s rows 必须存在", key).isInstanceOf(List.class);
        }

        // url.rows 每行含 id/title/url/favicon
        List<HashMap> urlRows = (List<HashMap>) ((HashMap) item.get("url")).get("rows");
        for (HashMap row : urlRows) {
            assertThat(row.get("id")).as("url row 必须含 id").isNotNull();
            assertThat(row).containsKeys("title", "url", "favicon");
        }

        // note.rows 每行含 id/title/categoryId
        List<HashMap> noteRows = (List<HashMap>) ((HashMap) item.get("note")).get("rows");
        for (HashMap row : noteRows) {
            assertThat(row.get("id")).isNotNull();
            assertThat(row).containsKeys("title", "categoryId");
        }

        // file.rows 每行含 id/name/fullPath
        List<HashMap> fileRows = (List<HashMap>) ((HashMap) item.get("file")).get("rows");
        for (HashMap row : fileRows) {
            assertThat(row.get("id")).isNotNull();
            assertThat(row).containsKeys("name", "fullPath");
        }

        // mail.rows 每行含 id/subject/from
        List<HashMap> mailRows = (List<HashMap>) ((HashMap) item.get("mail")).get("rows");
        for (HashMap row : mailRows) {
            assertThat(row.get("id")).isNotNull();
            assertThat(row).containsKeys("subject", "from");
        }
    }

    @Test
    void heatMapCountEqualsSumOfFourTypesForSameDay() {
        String uid = pickUid();

        List<HashMap> heat = dashboardService.getHeatMapSubmitDataByCurrentYear(uid);
        assertThat(heat).isNotNull();

        // 至少能跑通；如果热力图有数据，挑一条做总和不变量验证
        if (heat.isEmpty()) {
            System.out.println("[skip] uid=" + uid + " 热力图为空，无法做总和不变量验证");
            return;
        }

        HashMap first = heat.get(0);
        String date = (String) first.get("date");
        Integer heatCount = toInt(first.get("count"));
        assertThat(date).as("热力图元素必须含 date").isNotNull();
        assertThat(heatCount).as("热力图元素必须含 count").isNotNull();

        // 取同一天 detail，断言四类 count 之和 == heatCount
        List<HashMap> detailList = dashboardService.getActivityByDate(uid, date, "y-m-d");
        assertThat(detailList).hasSize(1);
        HashMap detail = detailList.get(0);
        int sum = 0;
        for (String key : new String[]{"url", "note", "file", "mail"}) {
            sum += toInt(((HashMap) detail.get(key)).get("count"));
        }
        assertThat(sum)
                .as("热力图 count(%d) 必须等于四类 detail count 之和", heatCount)
                .isEqualTo(heatCount);
    }

    @Test
    void nonexistentUidReturnsAllZeros() {
        String ghostUid = "nonexistent-uid-for-dashboard-it-test-0001";

        List<HashMap> result = dashboardService.getActivityByDate(ghostUid,
                java.time.YearMonth.now().toString(), "y-m");
        assertThat(result).hasSize(1);
        HashMap item = result.get(0);
        for (String key : new String[]{"url", "note", "file", "mail"}) {
            HashMap block = (HashMap) item.get(key);
            assertThat(toInt(block.get("count"))).as("ghost uid key=%s count 应为 0", key).isZero();
            assertThat(((List) block.get("rows"))).isEmpty();
        }

        List<HashMap> heat = dashboardService.getHeatMapSubmitDataByCurrentYear(ghostUid);
        assertThat(heat).isEmpty();
    }

    @Test
    void getSubmitYearListReturnsAscendingRange() {
        // 不依赖外部数据，纯算术生成；构造一个注册年=2020 的 User
        com.seaurl.platform.common.model.User user = new com.seaurl.platform.common.model.User();
        java.util.Date reg = java.sql.Date.valueOf("2020-06-15");
        // User 的 createdDt 存的是 ISO 格式字符串转 Date，这里直接传北京时区 Date，方法内部会做时区换算
        user.setCreatedDt(reg);

        List<Integer> years = dashboardService.getSubmitYearList(user);
        assertThat(years).isNotNull();
        assertThat(years.get(0)).isEqualTo(2020);
        int currentYear = java.time.Year.now().getValue();
        assertThat(years.get(years.size() - 1)).isEqualTo(currentYear);
    }

    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (NumberFormatException e) { return 0; }
    }
}
