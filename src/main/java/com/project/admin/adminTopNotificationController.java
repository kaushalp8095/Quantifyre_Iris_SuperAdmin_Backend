package com.project.admin;

import com.project.common.service.AdminNotificationService;
import com.project.common.dto.adminNotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/top-notifications")
@CrossOrigin(origins = "http://127.0.0.1:5500", allowCredentials = "true")
public class adminTopNotificationController {

    @Autowired
    private AdminNotificationService notifService;

    @GetMapping("/get/{adminId}")
    public ResponseEntity<?> getNotifications(@PathVariable Long adminId) {
        long unreadCount = notifService.getUnreadCount(adminId);
        List<adminNotificationDTO> notifs = notifService.getNotificationsForAdmin(adminId);
        
        return ResponseEntity.ok(Map.of(
                "unreadCount", unreadCount,
                "notifications", notifs
        ));
    }

    @PostMapping("/mark-read/{adminId}")
    public ResponseEntity<?> markAllAsRead(@PathVariable Long adminId) {
        notifService.markAllAsRead(adminId);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    // Testing ke liye endpoint (Postman/Browser se call karke test kar sakte ho)
    @GetMapping("/create-test/{adminId}")
    public ResponseEntity<?> createTestNotif(
            @PathVariable Long adminId,
            @RequestParam String type, 
            @RequestParam String title, 
            @RequestParam String msg,
            @RequestParam String toggleKey) { // Example: IN_APP_NEW_MSG
        
        notifService.createNotificationWithCheck(adminId, type, title, msg, toggleKey);
        return ResponseEntity.ok(Map.of("message", "Test Triggered. Check console if blocked!"));
    }
}