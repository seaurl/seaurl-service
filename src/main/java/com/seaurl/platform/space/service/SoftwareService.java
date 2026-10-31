package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.Software;
import com.seaurl.platform.space.model.SoftwareGroup;
import com.seaurl.platform.space.model.UserSoftware;

import java.util.List;

public interface SoftwareService {

    boolean add(UserSoftware userSoftware);

    boolean remove(String uid, String softwareId);

    List<Software> queryById(String uid);

    UserSoftware queryBy(String uid, String softwareId);

    List<SoftwareGroup> queryAll();

    List<Software> queryByParam(String softwareGroupId,  String param);
}
