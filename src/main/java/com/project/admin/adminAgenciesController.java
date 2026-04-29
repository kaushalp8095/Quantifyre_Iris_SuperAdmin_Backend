package com.project.admin;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dto.adminAgenciesStatsdto;
import com.project.common.models.adminAddAgenciesModel;
import com.project.common.service.AdminActivityLogService;
import com.project.common.service.AgencyService;
import com.project.common.service.SupabaseStorageService;

@RestController
@RequestMapping("/api/admin/agencies")
@CrossOrigin(origins = "*")
public class adminAgenciesController {

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private SupabaseStorageService storageService;

    @Autowired
    private AdminActivityLogService logService; // ✅ LOG SERVICE

    // IP helper
    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip;
    }

    // ==========================================
    // 1. ADD NEW AGENCY
    // ==========================================
    @PostMapping("/add")
    public ResponseEntity<?> addAgency(
            @RequestParam("agencyData") String agencyJson,
            @RequestParam(value = "agencyLogo", required = false) MultipartFile logo,
            @RequestParam("adminId") Long adminId,
            HttpServletRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            adminAddAgenciesModel agency = mapper.readValue(agencyJson, adminAddAgenciesModel.class);

            if (logo != null && !logo.isEmpty()) {
                String publicImageUrl = storageService.uploadFile(logo, "agency-logos");
                agency.setAgencyLogo(publicImageUrl);
            }

            agencyService.saveAgency(agency, adminId);

            // ✅ LOG
            logService.log(adminId, "Super Admin", "ADMIN",
                    "Created Agency: " + agency.getAgencyName(), "Agency", getIp(request), "SUCCESS");

            return ResponseEntity.ok(Map.of("message", "Agency Created Successfully!", "status", "success"));

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ==========================================
    // 2. UPDATE AGENCY
    // ==========================================
    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateAgency(
            @PathVariable Long id,
            @RequestParam("agencyData") String agencyJson,
            @RequestParam(value = "agencyLogo", required = false) MultipartFile logo,
            @RequestParam(value = "adminId", required = false) Long adminId,
            HttpServletRequest request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            adminAddAgenciesModel updatedData = mapper.readValue(agencyJson, adminAddAgenciesModel.class);

            return agencyService.getAgencyById(id).map(agency -> {
                agency.setId(id);
                agency.setAgencyName(updatedData.getAgencyName());
                agency.setOwnerName(updatedData.getOwnerName());
                agency.setEmail(updatedData.getEmail());
                agency.setPhoneNumber(updatedData.getPhoneNumber());
                if (updatedData.getPassword() != null && !updatedData.getPassword().trim().isEmpty()
                        && !updatedData.getPassword().equals("********")) {
                    agency.setPassword(updatedData.getPassword());
                }
                agency.setPlan(updatedData.getPlan());
                agency.setStatus(updatedData.getStatus());
                agency.setAddress(updatedData.getAddress());
                agency.setCountry(updatedData.getCountry());
                agency.setState(updatedData.getState());
                agency.setCity(updatedData.getCity());
                agency.setPincode(updatedData.getPincode());

                if (logo != null && !logo.isEmpty()) {
                    try {
                        String newPublicUrl = storageService.uploadFile(logo, "agency-logos");
                        agency.setAgencyLogo(newPublicUrl);
                    } catch (Exception e) {
                        System.err.println("❌ Logo Upload Failed: " + e.getMessage());
                    }
                }

                agencyService.saveAgency(agency, adminId);

                // ✅ LOG
                logService.log(adminId, "Super Admin", "ADMIN",
                        "Updated Agency: " + agency.getAgencyName(), "Agency", getIp(request), "SUCCESS");

                return ResponseEntity.ok(Map.of("message", "Agency Updated Successfully!", "status", "success"));
            }).orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    // ==========================================
    // 3. LIST & DETAILS
    // ==========================================
    @GetMapping("/list")
    public ResponseEntity<List<adminAgenciesStatsdto>> getAgenciesList(@RequestParam Long adminId) {
        return ResponseEntity.ok(agencyService.getAllAgenciesForTable(adminId));
    }

    @GetMapping("/details/{id}")
    public ResponseEntity<adminAddAgenciesModel> getAgencyDetails(@PathVariable Long id) {
        return agencyService.getAgencyById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // 4. DELETE AGENCY
    // ==========================================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAgency(@PathVariable Long id,
                                           @RequestParam(required = false) Long adminId,
                                           HttpServletRequest request) {
        try {
            // Name pehle lo (delete ke baad available nahi hogi)
            String agencyName = agencyService.getAgencyById(id)
                    .map(a -> a.getAgencyName()).orElse("ID: " + id);

            agencyService.deleteAgency(id);

            // ✅ LOG
            if (adminId != null) {
                logService.log(adminId, "Super Admin", "ADMIN",
                        "Deleted Agency: " + agencyName, "Agency", getIp(request), "SUCCESS");
            }

            return ResponseEntity.ok(Map.of("message", "Agency Deleted Successfully!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }
}