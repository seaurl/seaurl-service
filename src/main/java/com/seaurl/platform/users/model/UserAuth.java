package com.seaurl.platform.users.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class UserAuth {
    //  id
    private String id;
    private String uid;
    private String userName;
    private String email;
    private String password;
    // 是否已验证
    private boolean isVerified;
    //是否是超级管理员
    private boolean isAdmin;

    //    状态 2正常用户 3禁言用户 4虚拟用户 5运营
    private Integer status;
    //角色ID
    private List<String> roles;
    private Date createdDt;//创建时间
    private Date updatedDt;//更新时间
}
