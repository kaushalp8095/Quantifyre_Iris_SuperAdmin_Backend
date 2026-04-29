package com.project.admin;

import com.project.common.models.adminActivityLogModel;
import com.project.common.service.AdminActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/activity-logs")
@CrossOrigin(origins = "*")
public class adminActivityLogsController {

    @Autowired
    private AdminActivityLogService logService;

    // ==========================================
    // 1. GET ALL LOGS (no filter)
    // GET /api/admin/activity-logs/list?adminId=1
    // ==========================================
    @GetMapping("/list")
    public ResponseEntity<List<adminActivityLogModel>> getLogs(@RequestParam Long adminId) {
        try {
            return ResponseEntity.ok(logService.getAllLogs(adminId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ==========================================
    // 2. SEARCH + FILTER
    // GET /api/admin/activity-logs/search
    //   ?adminId=1
    //   &search=login         (optional)
    //   &dateRange=LAST_7     (optional: TODAY/YESTERDAY/LAST_7/LAST_30/THIS_MONTH/CUSTOM)
    //   &customStart=2025-07-01  (sirf CUSTOM ke liye)
    //   &customEnd=2025-07-15    (sirf CUSTOM ke liye)
    // ==========================================
    @GetMapping("/search")
    public ResponseEntity<List<adminActivityLogModel>> searchLogs(
            @RequestParam Long adminId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateRange,
            @RequestParam(required = false) String customStart,
            @RequestParam(required = false) String customEnd) {
        try {
            List<adminActivityLogModel> result = logService.searchLogs(
                    adminId, search, dateRange, customStart, customEnd);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ==========================================
    // 3. MANUAL LOG ADD (testing + internal use)
    // POST /api/admin/activity-logs/add
    // ==========================================
    @PostMapping("/add")
    public ResponseEntity<?> addLog(@RequestBody adminActivityLogModel log) {
        try {
            logService.saveLog(log);
            return ResponseEntity.ok(Map.of("message", "Log saved", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}