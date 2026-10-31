package com.seaurl.platform.users.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AccountNotice {
    private String userName;
    // 系统通知
    private List<String> notices;
}
