package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

// 等级表
@Setter
@Getter
public class Level {
    private String id;

    // 最高积分
    private int maxPoints;

    // 最低积分
    private int minPoints;

    //等级
    private int level;

    private Date createdDt;//创建时间
    private Date updatedDt;
}

//1-99    等级1
//
//100-299    等级2
//
//300-599    等级3
//
//600-999    等级4
//
//1000-1499    等级5
//
//1500-2099    等级6
//
//2100-2799    等级7
//
//2800-3599    等级8
//
//3600-4499    等级9
//
//4500-5499    等级10
//
//5500-6599    等级11
//
//6600-7799    等级12
//
//7800-9099    等级13
//
//10000-13999    等级14
//
//14000-15499   等级15