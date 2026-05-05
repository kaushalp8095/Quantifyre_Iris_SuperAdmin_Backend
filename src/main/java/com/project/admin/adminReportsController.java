package com.project.admin;

import com.project.common.models.adminReportModel;
import com.project.common.models.adminScheduledReportModel;
import com.project.common.service.AdminActivityLogService;
import com.project.common.service.AdminReportService;
import com.project.common.service.AgencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "*")
public class adminReportsController {

    @Autowired private AdminReportService reportService;
    @Autowired private AgencyService agencyService;
    @Autowired private AdminActivityLogService logService;

    private String getIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        return (ip == null || ip.isEmpty()) ? req.getRemoteAddr() : ip;
    }

    // ==========================================
    // 1. MAIN REPORTS PAGE — stats + recent list + chart data
    // GET /api/admin/reports/dashboard?adminId=1
    // ==========================================
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam Long adminId) {
        try {
            Map<String, Object> stats  = reportService.getStats(adminId);
            Map<String, Object> charts = reportService.getChartData(adminId);
            List<adminReportModel> recent = reportService.getAllReports(adminId);

            // Recent 10 reports ke liye clean list
            List<Map<String, Object>> recentList = new ArrayList<>();
            recent.stream().limit(10).forEach(r -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",          r.getId());
                row.put("reportName",  r.getReportName());
                row.put("generatedBy", r.getGeneratedBy());
                row.put("generatedOn", r.getGeneratedOn());
                row.put("reportType",  r.getReportType());
                row.put("status",      r.getStatus());
                row.put("format",      r.getFormat());
                recentList.add(row);
            });

            Map<String, Object> response = new LinkedHashMap<>();
            response.putAll(stats);
            response.put("recentReports", recentList);
            response.put("charts", charts);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 2. GENERATE NEW REPORT
    // POST /api/admin/reports/generate
    // Body: { adminId, reportType, startDate, endDate, format }
    // ==========================================
    @PostMapping("/generate")
    public ResponseEntity<?> generateReport(@RequestBody Map<String, Object> body,
                                             HttpServletRequest request) {
        try {
            Long adminId = Long.parseLong(body.get("adminId").toString());
            String type  = (String) body.getOrDefault("reportType", "Performance Report");
            String fmt   = (String) body.getOrDefault("format", "PDF");
            String startStr = (String) body.get("startDate");
            String endStr   = (String) body.get("endDate");

            adminReportModel report = new adminReportModel();
            report.setAdminId(adminId);
            report.setReportType(type);
            report.setReportName(type + " - " + java.time.LocalDate.now());
            report.setFormat(fmt);
            report.setStatus("Generated");
            report.setGeneratedBy("Super Admin");

            if (startStr != null && !startStr.isBlank())
                report.setStartDate(java.time.LocalDate.parse(startStr));
            if (endStr != null && !endStr.isBlank())
                report.setEndDate(java.time.LocalDate.parse(endStr));

            // Aggregated data generate karo (agency/campaign data se)
            String reportData = buildReportData(adminId, type, report.getStartDate(), report.getEndDate());
            report.setReportData(reportData);

            adminReportModel saved = reportService.saveReport(report);

            logService.log(adminId, "Super Admin", "ADMIN",
                    "Generated Report: " + saved.getReportName(), "Reports", getIp(request), "SUCCESS");

            return ResponseEntity.ok(Map.of(
                    "message",  "Report Generated Successfully!",
                    "reportId", saved.getId(),
                    "status",   "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 3. REPORT DETAILS (ReportDetails page)
    // GET /api/admin/reports/details/{id}
    // ==========================================
    @GetMapping("/details/{id}")
    public ResponseEntity<?> getReportDetails(@PathVariable Long id) {
        return reportService.getReportById(id)
                .map(report -> {
                    Map<String, Object> res = new LinkedHashMap<>();
                    res.put("id",          report.getId());
                    res.put("reportName",  report.getReportName());
                    res.put("reportType",  report.getReportType());
                    res.put("generatedBy", report.getGeneratedBy());
                    res.put("generatedOn", report.getGeneratedOn());
                    res.put("status",      report.getStatus());
                    res.put("format",      report.getFormat());
                    res.put("startDate",   report.getStartDate());
                    res.put("endDate",     report.getEndDate());

                    // Parse reportData JSON
                    try {
                        if (report.getReportData() != null) {
                            Map<?, ?> data = new com.fasterxml.jackson.databind.ObjectMapper()
                                    .readValue(report.getReportData(), Map.class);
                            res.put("reportData", data);
                        }
                    } catch (Exception e) {
                        res.put("reportData", Map.of());
                    }
                    return ResponseEntity.ok(res);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // 4. DELETE REPORT
    // ==========================================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable Long id,
                                           @RequestParam(required = false) Long adminId,
                                           HttpServletRequest request) {
        try {
            reportService.deleteReport(id);
            if (adminId != null) logService.log(adminId, "Super Admin", "ADMIN",
                    "Deleted Report #" + id, "Reports", getIp(request), "SUCCESS");
            return ResponseEntity.ok(Map.of("message", "Report Deleted!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 5. AGENCY REPORT (AdminAgencyReport.html)
    // GET /api/admin/reports/agency?adminId=1&month=&status=
    // ==========================================
    @GetMapping("/agency")
    public ResponseEntity<?> getAgencyReport(
            @RequestParam Long adminId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status) {
        try {
            List<com.project.common.models.adminAddAgenciesModel> agencies =
                    agencyService.getAgenciesByAdminId(adminId);

            // Filter by status
            if (status != null && !status.isBlank() && !"All".equalsIgnoreCase(status)) {
                agencies.removeIf(a -> !status.equalsIgnoreCase(a.getStatus()));
            }

            // Agency chart data — agencies by plan
            Map<String, Long> planMap = new LinkedHashMap<>();
            for (var a : agencies) {
                String plan = a.getPlan() != null ? a.getPlan() : "Basic";
                planMap.merge(plan, 1L, Long::sum);
            }

            // Agency table rows
            List<Map<String, Object>> rows = new ArrayList<>();
            for (var a : agencies) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",          a.getId());
                row.put("agencyName",  a.getAgencyName());
                row.put("plan",        a.getPlan() != null ? a.getPlan() : "Basic");
                row.put("status",      a.getStatus() != null ? a.getStatus() : "Pending");
                row.put("agencyLogo",  a.getAgencyLogo());
                rows.add(row);
            }

            return ResponseEntity.ok(Map.of(
                    "agencies",    rows,
                    "chartLabels", new ArrayList<>(planMap.keySet()),
                    "chartCounts", new ArrayList<>(planMap.values()),
                    "total",       agencies.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 6. CLIENT REPORT (AdminClientReport.html)
    // GET /api/admin/reports/client?adminId=1
    // ==========================================
    @GetMapping("/client")
    public ResponseEntity<?> getClientReport(@RequestParam Long adminId) {
        try {
            // Agency ke through clients
            List<com.project.common.models.adminAddAgenciesModel> agencies =
                    agencyService.getAgenciesByAdminId(adminId);

            // Simple chart — total clients per agency (mock from agencies list)
            List<String> agencyNames   = new ArrayList<>();
            List<Long>   clientCounts  = new ArrayList<>();

            for (var a : agencies) {
                agencyNames.add(a.getAgencyName());
                // TotalClients agar available nahi to 0
                clientCounts.add(0L);
            }

            return ResponseEntity.ok(Map.of(
                    "chartLabels", agencyNames,
                    "chartCounts", clientCounts,
                    "total",       agencies.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 7. SCHEDULED REPORTS
    // ==========================================
    @GetMapping("/scheduled/list")
    public ResponseEntity<?> getSchedules(@RequestParam Long adminId) {
        try {
            return ResponseEntity.ok(reportService.getSchedules(adminId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/scheduled/add")
    public ResponseEntity<?> addSchedule(@RequestBody adminScheduledReportModel schedule,
                                          HttpServletRequest request) {
        try {
            adminScheduledReportModel saved = reportService.saveSchedule(schedule);
            logService.log(schedule.getAdminId(), "Super Admin", "ADMIN",
                    "Created Schedule: " + saved.getScheduleName(), "Reports", getIp(request), "SUCCESS");
            return ResponseEntity.ok(Map.of("message", "Schedule Created!", "id", saved.getId(), "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/scheduled/delete/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id,
                                             @RequestParam(required = false) Long adminId,
                                             HttpServletRequest request) {
        try {
            reportService.deleteSchedule(id);
            if (adminId != null) logService.log(adminId, "Super Admin", "ADMIN",
                    "Deleted Schedule #" + id, "Reports", getIp(request), "SUCCESS");
            return ResponseEntity.ok(Map.of("message", "Schedule Deleted!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // HELPER: Report Data Build
    // ==========================================
    private String buildReportData(Long adminId, String type, java.time.LocalDate start, java.time.LocalDate end) {
        try {
            List<adminReportModel> past = reportService.getAllReports(adminId);
            long total = past.size();
            // Simple aggregated numbers from existing reports
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("totalReports",  total);
            data.put("totalSpend",    total * 6800L);   // Demo calculation
            data.put("conversions",   total * 48L);
            data.put("avgCost",       total > 0 ? 6800 : 0);
            data.put("reportType",    type);
            data.put("dateRange",     (start != null ? start : "") + " to " + (end != null ? end : ""));

            // Trend chart data — last 6 months
            List<String> months  = new ArrayList<>();
            List<Long>   counts  = new ArrayList<>();
            for (int i = 5; i >= 0; i--) {
                months.add(java.time.LocalDate.now().minusMonths(i)
                        .getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
                counts.add((long)(Math.random() * 20 + 5));
            }
            data.put("trendMonths", months);
            data.put("trendCounts", counts);

            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }
}