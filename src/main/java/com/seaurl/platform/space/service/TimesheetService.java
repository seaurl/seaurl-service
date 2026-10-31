package com.seaurl.platform.space.service;

import com.seaurl.platform.space.model.Timesheet;

import java.util.List;

public interface TimesheetService {
    List<Timesheet> queryBy(String uid, Integer year, Integer month);

    List<Timesheet> search(String uid, String keyword, Integer year, Integer month, Boolean global);

    Timesheet addTask(String uid, String name, Integer year, Integer month, Integer status, Double outputValue, String category, Double totalHours);

    boolean updateHour(String taskId, Integer day, Double hours);

    boolean updateOutput(String taskId, Integer day, Double value);

    boolean deleteTask(String taskId);

    boolean renameTask(String taskId, String name);

    boolean updateStatus(String taskId, Integer status);

    boolean updateTask(String taskId, String name, Integer status, Double outputValue, String category, Double totalHours);

    Integer copyLastMonth(String uid, Integer year, Integer month);

    List<String> categories(String uid);
}
