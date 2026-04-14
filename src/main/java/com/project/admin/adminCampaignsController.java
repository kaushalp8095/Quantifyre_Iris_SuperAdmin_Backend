package com.project.admin;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.project.common.service.CampaignService;

@RestController
@RequestMapping("/api/admin/campaigns")
@CrossOrigin(origins = "*")
public class adminCampaignsController {

    @Autowired
    private CampaignService campaignService;

    // ==========================================
    // 1. GET CAMPAIGNS LIST (Admin Filtered)
    // ==========================================
    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> getAllCampaignsList(@RequestParam Long adminId) { // 👈 adminId accept kiya
        try {
            // Ab Service ko adminId pass kar rahe hain
            List<Map<String, Object>> campaigns = campaignService.getAllCampaignsForAdmin(adminId);
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            System.err.println("Error fetching campaigns: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
    
    // ==========================================
    // 2. DELETE CAMPAIGN (ADMIN)
    // ==========================================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCampaign(@PathVariable Long id) {
        try {
            boolean isDeleted = campaignService.deleteCampaignByAdmin(id);
            if (isDeleted) {
                return ResponseEntity.ok(Map.of("message", "Campaign Deleted Successfully!", "status", "success"));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Campaign not found in database."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }
}