package com.project.admin;

import com.project.common.models.adminLoginHistory;
import com.project.common.models.adminLoginModel;
import com.project.common.service.AdminLoginService;
import com.project.common.service.EmailService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse; // Import zaroori hai
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5500","https://quantifyre-iris-super-admin.vercel.app"}, allowCredentials = "true") // 🔴 DHYAN DEIN: Cookie ke liye origin fix aur allowCredentials true hona chahiye
public class adminLoginController {

    @Autowired
    private AdminLoginService adminService;
    
    @Autowired
    private EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody Map<String, String> credentials, HttpServletResponse res, HttpServletRequest request) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        // Null or Empty checks
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", "Email and password are required"));
        }

        // Service call to find admin
        Optional<adminLoginModel> adminOpt = adminService.findByEmail(email);

        if (adminOpt.isPresent()) {
            adminLoginModel admin = adminOpt.get();

            // 🔴 Password Match Check
            if (admin.getPassword().equals(password)) {
                
                // Cookie Setup
            	// 1. COOKIE SETUP (SameSite=None; Secure zaroori hai Vercel ke liye)
                String cookieValue = admin.getId() + "_" + admin.getEmail();
                String cookieHeader = String.format("admin_session=%s; Path=/; HttpOnly; SameSite=None; Secure; Max-Age=%d", 
                                        cookieValue, 24 * 60 * 60); // 24 hours
                res.setHeader("Set-Cookie", cookieHeader);
                
                try {
                    adminLoginHistory history = new adminLoginHistory();
                    history.setAdminId(admin.getId());
                    history.setEmail(admin.getEmail());
                    history.setLoginTime(LocalDateTime.now());
                    
                    // IP Address nikalne ka logic
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                        ip = request.getRemoteAddr();
                    }
                    history.setIpAddress(ip);

                    // Device Info (User-Agent)
                    String userAgent = request.getHeader("User-Agent");
                    history.setDeviceInfo(parseUserAgent(userAgent)); // Helper niche hai
                    
                    // Location (Filhaal static, aap chaho toh IP-API integrate kar sakte ho)
                    history.setLocation("IP Logged: " + ip); 

                    // Service Call (Jo SQL + Mongo dono mein save karega)
                    adminService.saveAdminLoginHistory(history);
                    
                    if (admin.isLoginAlertsEnabled()) {
                        String finalIp = history.getIpAddress();
                        new Thread(() -> {
                            try {
                                // Agency wale method ko hi use kar rahe hain (Agar same parameters hain)
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
                } catch (Exception e) {
                    System.err.println("❌ History Error: " + e.getMessage());
                }

                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Login Successful");
                response.put("adminId", admin.getId());
                response.put("name", admin.getFirstName() + " " + admin.getLastName());
                response.put("profileLogo", admin.getProfileLogo());
                response.put("email", admin.getEmail());
                response.put("loginTime", System.currentTimeMillis());

                return ResponseEntity.ok(response);
            } else {
                // 🔴 WRONG PASSWORD - Return 401 Unauthorized
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "*Invalid Credentials!"));
            }
        } else {
            // 🔴 WRONG EMAIL - Return 404 Not Found
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "*Invalid Credentials!"));
        }
    }
    
    @GetMapping("/security/login-activity/{id}")
    public ResponseEntity<?> getLoginActivity(@PathVariable Long id) {
        List<adminLoginHistory> history = adminService.getAdminLoginHistory(id);
        return ResponseEntity.ok(history);
    }
    
    
 // 💡 Helper method for User Agent (Isko Controller ke end mein daal dena)
    private String parseUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Chrome")) return "Chrome on Windows";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("iPhone")) return "Safari on iPhone";
        return "Web Browser";
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse res) {
        // Cookie ko expire karne ke liye Max-Age 0 set karein
        String cookieHeader = "admin_session=; Path=/; HttpOnly; SameSite=None; Secure; Max-Age=0";
        res.setHeader("Set-Cookie", cookieHeader);
        
        return ResponseEntity.ok(Map.of("status", "success", "message", "Logged out"));
    }
    
}