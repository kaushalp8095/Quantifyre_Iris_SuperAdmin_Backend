package com.project.admin;

import com.project.common.models.adminLoginModel;
import com.project.common.models.agencyNotificationSettings;
import com.project.common.service.AdminLoginService;
import com.project.common.service.SupabaseStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/settings")
@CrossOrigin(origins = "http://127.0.0.1:5500", allowCredentials = "true")
public class adminSettingController {

    @Autowired
    private AdminLoginService adminService;

    @Autowired
    private SupabaseStorageService storageService;

    // 1. Get Admin Details
    @GetMapping("/profile/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        Optional<adminLoginModel> adminOpt = adminService.getAdminById(id);
        if (adminOpt.isPresent()) {
            return ResponseEntity.ok(adminOpt.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Admin not found"));
    }

    // 2. Update Text Details
    @PutMapping("/profile/update/{id}")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @RequestBody Map<String, String> data) {
        try {
            adminLoginModel updatedAdmin = adminService.updateAdminProfile(id, data.get("firstName"), data.get("lastName"), data.get("phoneNumber"));
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully!", "admin", updatedAdmin));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

 // 3. Upload, Update & Delete Old Logo
    @PostMapping("/profile/upload-logo/{id}")
    public ResponseEntity<?> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            // A. Pehle DB se purana logo URL nikal lo
            Optional<adminLoginModel> adminOpt = adminService.getAdminById(id);
            String oldLogoUrl = null;
            if (adminOpt.isPresent()) {
                oldLogoUrl = adminOpt.get().getProfileLogo();
            }

            // B. Naya logo Supabase S3 mein upload karo
            String fileUrl = storageService.uploadFile(file, "admin-logos");
            
            // C. Database mein naya URL save karo
            adminService.updateProfileLogo(id, fileUrl);
            
            // D. Purana logo Supabase storage se DELETE kardo 
              if (oldLogoUrl != null && !oldLogoUrl.isEmpty() && oldLogoUrl.startsWith("http")) {
                storageService.deleteFileFromUrl(oldLogoUrl);
            }
            
            return ResponseEntity.ok(Map.of("message", "Logo updated and old logo deleted!", "logoUrl", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // 4. Change Password
    @PutMapping("/security/change-password/{id}")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> passwords) {
        try {
            boolean success = adminService.updatePassword(id, passwords.get("oldPassword"), passwords.get("newPassword"));
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Incorrect current password"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/security/toggle-alerts/{id}")
    public ResponseEntity<?> toggleAlerts(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            // Agar frontend se "enabled" key missing hai toh error handle karein
            if (!data.containsKey("enabled") || data.get("enabled") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Key 'enabled' is missing"));
            }

            boolean isEnabled = Boolean.parseBoolean(data.get("enabled").toString());
            adminService.updateLoginAlertStatus(id, isEnabled);
            return ResponseEntity.ok(Map.of("message", "Success"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
    
 // 6. Get Recent Login Activity (Dual DB Fetch - SQL)
    @GetMapping("/security/login-activity/{id}")
    public ResponseEntity<?> getLoginActivity(@PathVariable Long id) {
        try {
            // Service se latest 10 login records mangwayein
            return ResponseEntity.ok(adminService.getAdminLoginHistory(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch activity: " + e.getMessage()));
        }
    }
    
 // 7. Get Notification Settings
    @GetMapping("/notifications/{id}")
    public ResponseEntity<?> getNotificationSettings(@PathVariable Long id) {
        Optional<adminLoginModel> adminOpt = adminService.getAdminById(id);
        
        if (adminOpt.isPresent()) {
            adminLoginModel admin = adminOpt.get();
            agencyNotificationSettings settings = admin.getNotificationSettings();
            
            // Agar settings object hi nahi hai (Null hai)
            if (settings == null) {
                // Naya object return karega jisme upar wali Default True values hongi
                return ResponseEntity.ok(new agencyNotificationSettings());
            }
            
            return ResponseEntity.ok(settings);
        }
        return ResponseEntity.status(404).body(Map.of("error", "Admin not found"));
    }

    @PutMapping("/notifications/update/{id}")
    public ResponseEntity<?> updateNotificationSettings(@PathVariable Long id, @RequestBody com.project.common.models.agencyNotificationSettings settings) {
        try {
            // Service ko call karo
            adminService.updateAdminNotificationSettings(id, settings);
            return ResponseEntity.ok(Map.of("message", "Notification settings updated successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}