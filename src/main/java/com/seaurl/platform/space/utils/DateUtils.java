package com.seaurl.platform.space.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

public class DateUtils {
    private static final String ISO_FORMAT_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    /**
     * mongo 日期查询isodate
     *
     * @param dateStr
     * @return
     */
    public static Date dateToISODate(String dateStr) {
        //T代表后面跟着时间，Z代表UTC统一时间
        Date date = formatD(dateStr);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setCalendar(new GregorianCalendar(new SimpleTimeZone(0, "GMT")));
        String isoDate = format.format(date);
        try {
            return format.parse(isoDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Date dateToISODate(Date date) {
        //T代表后面跟着时间，Z代表UTC统一时间
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        format.setCalendar(new GregorianCalendar(new SimpleTimeZone(0, "GMT")));
        String isoDate = format.format(date);
        try {
            return format.parse(isoDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 时间格式(yyyy-MM-dd HH:mm:ss)
     */
    public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

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
     * 转换时区
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
     * 验证格式是否正确
     * @param date
     * @param format
     * @return
     */
    public static boolean isValidDate(String date, String format) {
        SimpleDateFormat df = new SimpleDateFormat(format);
        try {
            df.parse(date);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
}
