package com.seaurl.platform.space.controller;

import com.seaurl.common.result.ResultInfo;
import com.seaurl.common.result.ResultStatus;
import com.seaurl.common.result.ResultSuccess;
import com.seaurl.platform.common.context.UserContextHolder;
import com.seaurl.platform.common.model.LoginUser;
import com.seaurl.platform.space.model.Timesheet;
import com.seaurl.platform.space.request.TimesheetRequest;
import com.seaurl.platform.space.service.TimesheetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/space")
public class TimesheetController {
    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    TimesheetService timesheetService;

    @GetMapping("/crud/timesheet/query")
    public ResponseEntity<?> query(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (year == null || month == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            List<Timesheet> result = timesheetService.queryBy(loginUser.getUid(), year, month);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet query exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/timesheet/search")
    public ResponseEntity<?> search(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "global", required = false) Boolean global) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<Timesheet> result = timesheetService.search(loginUser.getUid(), keyword, year, month, global);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet search exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/timesheet/categories")
    public ResponseEntity<?> categories() {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        try {
            List<String> result = timesheetService.categories(loginUser.getUid());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet categories exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/addTask")
    public ResponseEntity<?> addTask(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getName())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Timesheet result = timesheetService.addTask(
                    loginUser.getUid(),
                    request.getName(),
                    request.getYear(),
                    request.getMonth(),
                    request.getStatus(),
                    request.getOutputValue(),
                    request.getCategory(),
                    request.getTotalHours());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet addTask exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/updateHour")
    public ResponseEntity<?> updateHour(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getTaskId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = timesheetService.updateHour(
                    request.getTaskId(),
                    request.getDay(),
                    request.getHours());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet updateHour exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/updateOutput")
    public ResponseEntity<?> updateOutput(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getTaskId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = timesheetService.updateOutput(
                    request.getTaskId(),
                    request.getDay(),
                    request.getOutputValue());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet updateOutput exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @GetMapping("/crud/timesheet/deleteTask")
    public ResponseEntity<?> deleteTask(@RequestParam("taskId") String taskId) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(taskId)) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = timesheetService.deleteTask(taskId);
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet deleteTask exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/renameTask")
    public ResponseEntity<?> renameTask(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getTaskId()) || !StringUtils.hasLength(request.getName())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = timesheetService.renameTask(request.getTaskId(), request.getName());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet renameTask exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/updateStatus")
    public ResponseEntity<?> updateStatus(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getTaskId()) || request.getStatus() == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = timesheetService.updateStatus(request.getTaskId(), request.getStatus());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet updateStatus exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/updateTask")
    public ResponseEntity<?> updateTask(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (!StringUtils.hasLength(request.getTaskId())) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            boolean result = timesheetService.updateTask(
                    request.getTaskId(),
                    request.getName(),
                    request.getStatus(),
                    request.getOutputValue(),
                    request.getCategory(),
                    request.getTotalHours());
            return ResponseEntity.ok(new ResultSuccess<>(result));
        } catch (Exception ex) {
            log.error("timesheet updateTask exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }

    @PostMapping("/crud/timesheet/copyLastMonth")
    public ResponseEntity<?> copyLastMonth(@RequestBody TimesheetRequest request) {
        LoginUser loginUser = UserContextHolder.getContext().getUser();
        if (loginUser == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.USER_NOT_FOUND));
        }
        if (request.getYear() == null || request.getMonth() == null) {
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.DATA_EMPTY));
        }
        try {
            Integer copied = timesheetService.copyLastMonth(loginUser.getUid(), request.getYear(), request.getMonth());
            return ResponseEntity.ok(new ResultSuccess<>(copied));
        } catch (Exception ex) {
            log.error("timesheet copyLastMonth exception={}", ex.getMessage());
            return ResponseEntity.ok(new ResultInfo<>(ResultStatus.Exception));
        }
    }
}
