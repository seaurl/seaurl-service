package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.Domain;

public interface DomainService {
    boolean addDomain(Domain domain);

    Domain queryDomainByName(String name);
}
