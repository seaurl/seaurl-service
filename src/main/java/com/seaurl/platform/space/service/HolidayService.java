package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.Holiday;

import java.util.List;

public interface HolidayService {

    List<Holiday> queryBy(String year);
}
