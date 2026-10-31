package com.seaurl.platform.space.utils;

public class Random {

    /**
     * 获取startnum到endnum之间的数字
     * @param startNum
     * @param endNum
     * @return
     */
    public static Integer getRandom(Integer startNum, Integer endNum){
        java.util.Random random = new java.util.Random();

        int lessNum = endNum - startNum;

        return random.nextInt(lessNum) + startNum;
    }
}
