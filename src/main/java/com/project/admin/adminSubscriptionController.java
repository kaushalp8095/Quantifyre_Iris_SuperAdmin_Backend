package com.project.admin;

import com.project.common.models.adminAddAgenciesModel;
import com.project.common.service.AdminActivityLogService;
import com.project.common.service.AgencyService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/subscriptions")
@CrossOrigin(origins = "*")
public class adminSubscriptionController {

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private AdminActivityLogService logService;

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip;
    }

    // ==========================================
    // 1. STATS + LIST
    // GET /api/admin/subscriptions/list?adminId=1
    // Returns: stats (total, active, pending) + agency list with plan/status
    // ==========================================
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getSubscriptionList(@RequestParam Long adminId) {
        try {
            List<adminAddAgenciesModel> agencies = agencyService.getAgenciesByAdminId(adminId);

            long total   = agencies.size();
            long active  = agencies.stream().filter(a -> "Active".equalsIgnoreCase(a.getStatus())).count();
            long pending = agencies.stream().filter(a -> "Pending".equalsIgnoreCase(a.getStatus())).count();

            // Frontend ke liye clean list
            List<Map<String, Object>> list = new ArrayList<>();
            for (adminAddAgenciesModel a : agencies) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id",          a.getId());
                row.put("agencyName",  a.getAgencyName());
                row.put("email",       a.getEmail());
                row.put("plan",        a.getPlan() != null ? a.getPlan() : "Basic");
                row.put("status",      a.getStatus() != null ? a.getStatus() : "Pending");
                row.put("agencyLogo",  a.getAgencyLogo());
                list.add(row);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("totalSubscriptions",   total);
            response.put("activeSubscriptions",  active);
            response.put("pendingSubscriptions", pending);
            response.put("subscriptions",        list);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 2. SINGLE SUBSCRIPTION DETAILS
    // GET /api/admin/subscriptions/details/{agencyId}
    // ==========================================
    @GetMapping("/details/{agencyId}")
    public ResponseEntity<?> getSubscriptionDetails(@PathVariable Long agencyId) {
        return agencyService.getAgencyById(agencyId).map(agency -> {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id",          agency.getId());
            response.put("agencyName",  agency.getAgencyName());
            response.put("ownerName",   agency.getOwnerName());
            response.put("email",       agency.getEmail());
            response.put("phoneNumber", agency.getPhoneNumber());
            response.put("address",     agency.getAddress());
            response.put("city",        agency.getCity());
            response.put("state",       agency.getState());
            response.put("country",     agency.getCountry());
            response.put("agencyLogo",  agency.getAgencyLogo());
            response.put("plan",        agency.getPlan() != null ? agency.getPlan() : "Basic");
            response.put("status",      agency.getStatus() != null ? agency.getStatus() : "Pending");
            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // 3. CANCEL SUBSCRIPTION
    // POST /api/admin/subscriptions/cancel/{agencyId}
    // Body: { "adminId": 1, "reason": "Too Expensive", "comments": "..." }
    // ==========================================
    @PostMapping("/cancel/{agencyId}")
    public ResponseEntity<?> cancelSubscription(
            @PathVariable Long agencyId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long adminId = body.get("adminId") != null
                    ? Long.parseLong(body.get("adminId").toString()) : null;
            String reason   = (String) body.getOrDefault("reason",   "Not specified");
            String comments = (String) body.getOrDefault("comments", "");

            return agencyService.getAgencyById(agencyId).map(agency -> {
                // Status update karo
                agency.setStatus("Cancelled");
                agencyService.saveAgency(agency, adminId);

                // Activity log
                if (adminId != null) {
                    String logMsg = "Cancelled Subscription: " + agency.getAgencyName()
                            + " | Reason: " + reason;
                    logService.log(adminId, "Super Admin", "ADMIN",
                            logMsg, "Subscription", getIp(request), "SUCCESS");
                }

                return ResponseEntity.ok(Map.of(
                        "message", "Subscription Cancelled Successfully!",
                        "status",  "success",
                        "agencyId", agencyId
                ));
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ==========================================
    // 4. REACTIVATE SUBSCRIPTION
    // POST /api/admin/subscriptions/reactivate/{agencyId}
    // ==========================================
    @PostMapping("/reactivate/{agencyId}")
    public ResponseEntity<?> reactivateSubscription(
            @PathVariable Long agencyId,
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        try {
            Long adminId = body.get("adminId") != null
                    ? Long.parseLong(body.get("adminId").toString()) : null;

            return agencyService.getAgencyById(agencyId).map(agency -> {
                agency.setStatus("Active");
                agencyService.saveAgency(agency, adminId);

                if (adminId != null) {
                    logService.log(adminId, "Super Admin", "ADMIN",
                            "Reactivated Subscription: " + agency.getAgencyName(),
                            "Subscription", getIp(request), "SUCCESS");
                }

                return ResponseEntity.ok(Map.of(
                        "message", "Subscription Reactivated Successfully!",
                        "status",  "success"
                ));
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}