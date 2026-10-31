package com.seaurl.platform.users.service;

import com.seaurl.platform.common.model.User;

import java.util.HashMap;
import java.util.List;

/**
 * Dashboard 提交历史聚合服务：四类（网址 / 笔记 / 文件 / 邮件）。
 * <p>
 * 是 dashboard 「提交历史」模块唯一的数据出口，{@link UrlService} 中对应的旧方法保留为冷代码。
 */
public interface DashboardService {

    /**
     * 热力图：当前年起一年范围内，按天返回四类当天数量总和。
     * 返回元素形如 {date:"YYYY-MM-DD", count:N}（count 为四类当天总和）。
     */
    List<HashMap> getHeatMapSubmitDataByCurrentYear(String uid);

    /**
     * 热力图：指定年全年范围内，按天返回四类当天数量总和。
     * 返回元素形如 {date:"YYYY-MM-DD", count:N}（count 为四类当天总和）。
     */
    List<HashMap> getHeatMapSubmitDataByYear(String uid, String year);

    /**
     * 提交年份列表（注册年 → 当前年，算术生成，不查 Mongo）。
     */
    List<Integer> getSubmitYearList(User user);

    /**
     * 详情：按日期取四类当天/当月/当年命中行，外层为单元素数组以兼容前端 push/replace。
     * 返回形如 [{date, url:{count,rows}, note:{count,rows}, file:{count,rows}, mail:{count,rows}}]。
     * 某一类聚合失败时该类返回 {count:0, rows:[]}，不影响其它三类。
     */
    List<HashMap> getActivityByDate(String uid, String date, String format);
}
