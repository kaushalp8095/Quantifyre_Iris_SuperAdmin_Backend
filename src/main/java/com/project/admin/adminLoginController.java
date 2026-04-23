package com.project.admin;

import com.project.common.models.adminLoginHistory;
import com.project.common.models.adminLoginModel;
import com.project.common.service.AdminLoginService;
import com.project.common.service.EmailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "https://quantifyre-iris-super-admin.vercel.app"}, allowCredentials = "true")
public class adminLoginController {

    @Autowired
    private AdminLoginService adminService;
    
    @Autowired
    private EmailService emailService;

    // ==========================================
    // MAIN LOGIN API
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> credentials, HttpServletResponse res, HttpServletRequest request) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Missing credentials"));
        }

        Optional<adminLoginModel> adminOpt = adminService.findByEmail(email.trim().toLowerCase());

        if (adminOpt.isPresent()) {
            adminLoginModel admin = adminOpt.get();

            // Password Match Check
            if (admin.getPassword().equals(password)) {
                
                // 1. Cookie Setup (UUID Token)
                setupCookies(res, admin.getId().toString());

                // 2. Login History & Alert Logic
                try {
                    adminLoginHistory history = new adminLoginHistory();
                    history.setAdminId(admin.getId());
                    history.setEmail(admin.getEmail());
                    history.setLoginTime(LocalDateTime.now());
                    
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
                    
                    history.setIpAddress(ip);
                    history.setDeviceInfo(parseUserAgent(request.getHeader("User-Agent")));
                    history.setLocation(fetchLocationFromIp(ip));

                    adminService.saveAdminLoginHistory(history);
                    
                    // 🟢 ALERTS LOGIC (Same as Agency Module)
                    if (admin.isLoginAlertsEnabled()) {
                        String finalIp = ip;
                        new Thread(() -> {
                            try {
                                emailService.sendLoginAlertEmail(
                                    admin.getEmail(), 
                                    history.getDeviceInfo(), 
                                    history.getLocation(), 
                                    finalIp
                                );
                                System.out.println("✅ Admin Login Alert Sent to: " + admin.getEmail());
                            } catch (Exception ex) {
                                System.err.println("❌ Admin Email Alert Failed: " + ex.getMessage());
                            }
                        }).start();
                    }
                    
                } catch (Exception e) { System.err.println("❌ History/Alert Error: " + e.getMessage()); }

                return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "adminId", admin.getId(),
                    "name", admin.getFirstName() + " " + admin.getLastName()
                ));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("status", "error", "message", "Invalid Credentials"));
    }

    // ==========================================
    // HELPER: COOKIE MANAGEMENT (Same as Agency)
    // ==========================================
    private void setupCookies(HttpServletResponse response, String adminId) {
        ResponseCookie cookie = ResponseCookie.from("admin_session", adminId)
                .httpOnly(true)
                .secure(true) // HTTPS zaroori hai
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None") // Cross-Domain Zaroori
                .build();
        
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // ==========================================
    // HELPER: LOCATION & DEVICE
    // ==========================================
    @SuppressWarnings("unchecked")
    private String fetchLocationFromIp(String ip) {
        // Localhost checking
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            return "Localhost (Dev)";
        }
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = "http://ip-api.com/json/" + ip;
            
            // API call to get city and country
            Map<String, Object> apiResponse = restTemplate.getForObject(url, Map.class);
            
            if (apiResponse != null && "success".equals(apiResponse.get("status"))) {
                return apiResponse.get("city") + ", " + apiResponse.get("country");
            }
        } catch (Exception e) {
            System.err.println("Location API Error: " + e.getMessage());
        }
        return "Unknown Location"; // Default fallback
    }

 // ==========================================
    // HELPER: USER AGENT PARSER (Chrome, Windows, etc.)
    // ==========================================
    private String parseUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) return "Unknown Device";
        
        String browser = "Web Browser";
        if (userAgent.contains("Edg/")) browser = "Edge";
        else if (userAgent.contains("Chrome/")) browser = "Chrome";
        else if (userAgent.contains("Firefox/")) browser = "Firefox";
        else if (userAgent.contains("Safari/") && !userAgent.contains("Chrome/")) browser = "Safari";

        String os = "Unknown OS";
        if (userAgent.contains("Windows NT 10.0")) os = "Windows 10/11";
        else if (userAgent.contains("Windows NT")) os = "Windows";
        else if (userAgent.contains("Android")) os = "Android";
        else if (userAgent.contains("iPhone")) os = "iOS";
        else if (userAgent.contains("Mac")) os = "MacOS";
        else if (userAgent.contains("Linux")) os = "Linux";
        
        return browser + " on " + os;
    }

    
    // ==========================================
    // LOGOUT
    // ==========================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from("admin_session", "")
                .httpOnly(true).secure(true).path("/").maxAge(0).sameSite("None").build();
        res.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}