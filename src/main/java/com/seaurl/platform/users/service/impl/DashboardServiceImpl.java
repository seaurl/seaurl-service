package com.seaurl.platform.users.service.impl;

import com.seaurl.platform.common.model.User;
import com.seaurl.platform.users.service.DashboardService;
import com.seaurl.platform.users.utils.DateUtils;
import com.seaurl.platform.users.utils.XDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.count;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

/**
 * Dashboard 提交历史聚合实现：四类（网址 / 笔记 / 文件 / 邮件）。
 * <p>
 * 复用 UrlServiceImpl 中验证过的 match → project($dateToString) → group(date).count() → project → sort 模板，
 * 抽出 {@link #countByDateRange} / {@link #countByYear} / {@link #rowsByDate} 三个私有 helper。
 * 时区硬编码 +08:00（与原实现一致）。
 */
@Service("dashboardService")
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private static final String TIMEZONE = "+08:00";

    /**
     * 四类聚合配置。
     * rowFields 为「列表展示 + 跳转所需」的最小字段集，不含 id（id 通过 _id toString 单独投影）。
     */
    private static final class TypeConfig {
        final String key;
        final String collection;
        final String dateField;
        final String[] rowFields;

        TypeConfig(String key, String collection, String dateField, String[] rowFields) {
            this.key = key;
            this.collection = collection;
            this.dateField = dateField;
            this.rowFields = rowFields;
        }
    }

    // 顺序固定：url / note / file / mail，影响详情返回字段顺序
    private static final List<TypeConfig> TYPES = new ArrayList<>();

    static {
        TYPES.add(new TypeConfig("url", "url", "createdDt", new String[]{"title", "url", "favicon"}));
        TYPES.add(new TypeConfig("note", "note", "createdDt", new String[]{"title", "categoryId"}));
        TYPES.add(new TypeConfig("file", "file", "createdDt", new String[]{"name", "fullPath"}));
        TYPES.add(new TypeConfig("mail", "mailMessage", "receivedDate", new String[]{"subject", "from"}));
    }

    @Autowired
    MongoTemplate mongoTemplate;

    @Override
    public List<HashMap> getHeatMapSubmitDataByCurrentYear(String uid) {
        String beforeDate = XDateUtils.beforeDate();
        String currentDate = XDateUtils.getCurrentDate();
        // 用 TreeMap 保证按日期升序
        Map<String, Integer> merged = new TreeMap<>();

        for (TypeConfig tc : TYPES) {
            try {
                Criteria base = Criteria.where("uid").is(uid).and("isDelete").is(false);
                Criteria range = new Criteria().andOperator(
                        Criteria.where(tc.dateField).gte(DateUtils.dateToISODate(beforeDate + " 00:00:00")),
                        Criteria.where(tc.dateField).lte(DateUtils.dateToISODate(currentDate + " 23:59:59"))
                );
                List<HashMap> list = countByDateRange(tc, base, range);
                for (HashMap row : list) {
                    String date = (String) row.get("date");
                    Integer count = toInt(row.get("count"));
                    if (date != null) {
                        merged.merge(date, count, Integer::sum);
                    }
                }
            } catch (Exception ex) {
                // 单类失败不影响其它三类
                log.warn("getHeatMapSubmitDataByCurrentYear collection={} err={}", tc.collection, ex.getMessage());
            }
        }
        return toList(merged);
    }

    @Override
    public List<HashMap> getHeatMapSubmitDataByYear(String uid, String year) {
        Map<String, Integer> merged = new TreeMap<>();
        for (TypeConfig tc : TYPES) {
            try {
                Criteria base = Criteria.where("uid").is(uid).and("isDelete").is(false);
                List<HashMap> list = countByYear(tc, base, year);
                for (HashMap row : list) {
                    String date = (String) row.get("date");
                    Integer count = toInt(row.get("count"));
                    if (date != null) {
                        merged.merge(date, count, Integer::sum);
                    }
                }
            } catch (Exception ex) {
                log.warn("getHeatMapSubmitDataByYear collection={} err={}", tc.collection, ex.getMessage());
            }
        }
        return toList(merged);
    }

    @Override
    public List<Integer> getSubmitYearList(User user) {
        Integer currentYear = XDateUtils.getCurrentYear();
        try {
            Date date = DateUtils.formatIsoTimeZoneToBeijingTimeZone(user.getCreatedDt());
            Integer registerYear = XDateUtils.formatDateToYear(date);
            List<Integer> yearList = new ArrayList<>();
            for (int year = registerYear; year <= currentYear; year++) {
                yearList.add(year);
            }
            return yearList;
        } catch (Exception ex) {
            log.info("getSubmitYearList error={}", ex.getMessage());
            return null;
        }
    }

    @Override
    public List<HashMap> getActivityByDate(String uid, String date, String format) {
        HashMap<String, Object> item = new LinkedHashMap<>();
        item.put("date", date);
        for (TypeConfig tc : TYPES) {
            item.put(tc.key, rowsByDate(tc, uid, date, format));
        }
        List<HashMap> result = new ArrayList<>();
        result.add(item);
        return result;
    }

    // ============ private helpers ============

    /**
     * 热力图按天分组计数：在给定日期范围内，返回 [{date, count}]。
     * 模板：match(base+range) → project($dateToString date) → group(date).count() → project → sort。
     */
    private List<HashMap> countByDateRange(TypeConfig tc, Criteria base, Criteria range) {
        Criteria matchCriteria = new Criteria().andOperator(base, range);

        ProjectionOperation projectDate = project(tc.dateField)
                .andExpression("{ $dateToString: {date: '$" + tc.dateField + "', format: '%Y-%m-%d', timezone: '" + TIMEZONE + "'}}").as("date");

        GroupOperation groupOp = group("date").count().as("count").first("date").as("date");

        ProjectionOperation projectOut = project("count", "date").and("date").previousOperation();

        SortOperation sortOp = sort(Sort.Direction.ASC, "date");

        Aggregation agg = Aggregation.newAggregation(
                match(matchCriteria),
                projectDate,
                groupOp,
                projectOut,
                sortOp
        );
        AggregationResults<HashMap> results = mongoTemplate.aggregate(agg, tc.collection, HashMap.class);
        return results.getMappedResults();
    }

    /**
     * 热力图按年分组计数：先 project 出 year 字符串，再 match 年份，最后按天 group。
     * 模板与原 UrlServiceImpl#getHeatMapSubmitDataByYear 一致（project 先于 match）。
     */
    private List<HashMap> countByYear(TypeConfig tc, Criteria base, String year) {
        ProjectionOperation projectDate = project(tc.dateField)
                .andExpression("{ $dateToString: {date: '$" + tc.dateField + "', format: '%Y-%m-%d', timezone: '" + TIMEZONE + "'}}").as("date")
                .andExpression("{ $dateToString: {date: '$" + tc.dateField + "', format: '%Y', timezone: '" + TIMEZONE + "'}}").as("year");

        Criteria matchCriteria = new Criteria().andOperator(base, Criteria.where("year").is(year));

        GroupOperation groupOp = group("date").count().as("count").first("date").as("date");

        ProjectionOperation projectOut = project("count", "date").and("date").previousOperation();

        SortOperation sortOp = sort(Sort.Direction.ASC, "date");

        Aggregation agg = Aggregation.newAggregation(
                projectDate,
                match(matchCriteria),
                groupOp,
                projectOut,
                sortOp
        );
        AggregationResults<HashMap> results = mongoTemplate.aggregate(agg, tc.collection, HashMap.class);
        return results.getMappedResults();
    }

    /**
     * 详情按日期取当天/当月/当年全部命中行：返回 {count:N, rows:[...]}。
     * 不再 facet limit 10，当天全部返回，前端做展开。
     * 失败时返回 {count:0, rows:[]}，不影响其它三类。
     */
    private HashMap rowsByDate(TypeConfig tc, String uid, String date, String format) {
        HashMap<String, Object> empty = new HashMap<>();
        empty.put("count", 0);
        empty.put("rows", new ArrayList<>());

        String dateFormat = formatToDateFmt(format);
        if (dateFormat == null) {
            return empty;
        }

        try {
            // 两段 match：第一段对真实字段 uid/isDelete（project 之前），
            // 第二段对 project 后的 date 字符串做精确匹配。
            MatchOperation preMatch = match(
                    Criteria.where("uid").is(uid).and("isDelete").is(false));

            // 投影：dateField（用于 sort）+ rowFields + id(_id toString) + date 字符串
            String[] includeFields = new String[tc.rowFields.length + 1];
            includeFields[0] = tc.dateField;
            System.arraycopy(tc.rowFields, 0, includeFields, 1, tc.rowFields.length);

            ProjectionOperation projectOp = project(includeFields)
                    .andExpression("toString(_id)").as("id")
                    .andExpression("{ $dateToString: {date: '$" + tc.dateField + "', format: '" + dateFormat + "', timezone: '" + TIMEZONE + "'}}").as("date");

            MatchOperation dateMatch = match(Criteria.where("date").is(date));

            SortOperation sortOp = sort(Sort.Direction.DESC, tc.dateField);

            Aggregation agg = Aggregation.newAggregation(preMatch, projectOp, dateMatch, sortOp);
            AggregationResults<HashMap> results = mongoTemplate.aggregate(agg, tc.collection, HashMap.class);
            List<HashMap> rows = results.getMappedResults();

            HashMap<String, Object> out = new HashMap<>();
            out.put("count", rows.size());
            out.put("rows", rows);
            return out;
        } catch (Exception ex) {
            log.warn("rowsByDate collection={} err={}", tc.collection, ex.getMessage());
            return empty;
        }
    }

    private static String formatToDateFmt(String format) {
        if ("y".equals(format)) return "%Y";
        if ("y-m".equals(format)) return "%Y-%m";
        if ("y-m-d".equals(format)) return "%Y-%m-%d";
        return null;
    }

    private static List<HashMap> toList(Map<String, Integer> merged) {
        List<HashMap> result = new ArrayList<>(merged.size());
        for (Map.Entry<String, Integer> e : merged.entrySet()) {
            HashMap<String, Object> m = new HashMap<>();
            m.put("date", e.getKey());
            m.put("count", e.getValue());
            result.add(m);
        }
        return result;
    }

    private static Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
