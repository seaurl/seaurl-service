package com.seaurl.platform.space.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class XDateUtils {
    private static Logger log = LoggerFactory.getLogger(DateUtils.class);

    // 默认的年月日的格式. yyyy-MM-dd
    private static final String PATTEN_DEFAULT_Y = "yyyy";
    private static final String PATTEN_DEFAULT_YMD = "yyyy-MM-dd";
    private static final String PATTEN_DEFAULT_YMD_HM = "yyyy-MM-dd HH:mm";
    private static final String PATTEN_DEFAULT_YMD_H = "yyyy-MM-dd HH";
    /**
     * 时间格式(yyyy-MM-dd HH:mm:ss)
     */
    public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    /**
     * 带T和Z的是标准时区
     */
    private static final String ISO_FORMAT_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final String ISO_FORMAT_DATE2 = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * 将iso date转换成日期格式
     * @param timestamp
     * @return
     */
    public static Date isoDateStrToDate(String timestamp) {
        // 使用标准的 ISO 8601 格式化器（自动解析时区）
        OffsetDateTime offsetDateTime = OffsetDateTime.parse(timestamp, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 转换为 Instant（标准 UTC 时间）
        Instant instant = offsetDateTime.toInstant();

        // 转换为 Date
        return Date.from(instant);
    }

    // timestamp是时间戳(到毫秒)
    //utc+8=cst
//    public static Date dateToISODate2(String timestamp) {
//        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(Long.parseLong(timestamp)).atZone(ZoneId.of("UTC"));
//
//        // 将ZonedDateTime转换为UTC的Instant对象
//        Instant utcInstant = zonedDateTime.toInstant();
//        Date date = Date.from(utcInstant);
//
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
//
//// 使用SimpleDateFormat对象将Date对象格式化为字符串，以UTC时区表示
//        String isoDate = sdf.format(date);
//        try {
//            Date r = sdf.parse(isoDate);
//            return r;
//        } catch (ParseException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    /**
     * mongo 日期查询isodate
     *
     * @param dateStr 时间字符串，台：2023-12-12 12:00:00
     * @return
     */
    public static Date dateToISODate(String dateStr) {
        //T代表后面跟着时间，Z代表UTC统一时间
        Date date = formatD(dateStr);
        SimpleDateFormat format = new SimpleDateFormat(ISO_FORMAT_DATE);
        format.setCalendar(
                // 在这里，做了时区转换
                new GregorianCalendar(new SimpleTimeZone(0, "GMT"))
        );
        String isoDate = format.format(date);
        try {
            return format.parse(isoDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Date formatD(String dateStr) {
        return formatD(dateStr, DATE_TIME_PATTERN);
    }

    public static Date formatD(String dateStr, String format) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        Date ret = null;
        try {
            ret = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            //
        }
        return ret;
    }

    /**
     * 将北京时区转换成ISO时区也就是-8h
     *
     * @param dateStr
     * @return
     */
    public static Date dateToISODate(Date dateStr) {
        try {
            // 解析字符串时间
            SimpleDateFormat format = new SimpleDateFormat(ISO_FORMAT_DATE);
            return format.parse(format.format(dateStr));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 将date转化成格林威治时间，再去判断是否
     *
     * @param date
     * @return
     */
    public static boolean isNow(Date date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(ISO_FORMAT_DATE);

        format.setCalendar(
                // 在这里，做了时区转换
                new GregorianCalendar(new SimpleTimeZone(0, "GMT"))
        );
        // 将日期转化为标准时间
        String isoDateStr = format.format(date);
        Date isoDate = format.parse(isoDateStr);
        // 当前时间
        Date now = new Date();
        // 精确到小时
        SimpleDateFormat sf = new SimpleDateFormat(PATTEN_DEFAULT_YMD_H);
        //获取今天的日期
        String nowDay = sf.format(now);
        //对比的时间
        String day = sf.format(isoDate);
        log.info("now={},nowDay={}", now, nowDay);
        log.info("isoDate={},isoDay={}", isoDate, day);
        return day.equals(nowDay);
    }

    /**
     * 转换时区
     *
     * @param date
     * @return
     * @throws ParseException
     */
    public static Date formatIsoTimeZoneToBeijingTimeZone(Date date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat(ISO_FORMAT_DATE);

        format.setCalendar(
                // 在这里，做了时区转换
                new GregorianCalendar(new SimpleTimeZone(0, "GMT"))
        );
        // 将日期转化为标准时间
        String isoDateStr = format.format(date);
        Date isoDate = format.parse(isoDateStr);
        return isoDate;
    }


    /**
     * 获取过去一年的当前时间
     *
     * @return
     */
    public static String beforeDate() {
        SimpleDateFormat format = new SimpleDateFormat(PATTEN_DEFAULT_YMD);
        Calendar c = Calendar.getInstance();
        //过去一年
        c.setTime(new Date());
        c.add(Calendar.YEAR, -1);
        Date y = c.getTime();
        return format.format(y);
    }

    public static String getCurrentDate() {
        SimpleDateFormat format = new SimpleDateFormat(PATTEN_DEFAULT_YMD);
        Date now = new Date();
        return format.format(now);
    }

    public static Integer getCurrentYear() {
        Calendar c = Calendar.getInstance();
        return c.get(Calendar.YEAR);
    }

    public static Integer formatDateToYear(Date date) {
        SimpleDateFormat format = new SimpleDateFormat(PATTEN_DEFAULT_Y);
        String year = format.format(date);
        return Integer.parseInt(year);
    }
}
