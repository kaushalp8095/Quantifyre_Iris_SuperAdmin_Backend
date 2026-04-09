package com.project.admin;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dto.adminAgenciesStatsdto;
import com.project.common.models.adminAddAgenciesModel;
import com.project.common.service.AgencyService;
import com.project.common.service.SupabaseStorageService; // 👈 Import required

@RestController
@RequestMapping("/api/admin/agencies")
@CrossOrigin(origins = "*")
public class adminAgenciesController {

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private SupabaseStorageService storageService; // 👈 S3 Service Inject kiya

    // ==========================================
    // 1. ADD NEW AGENCY
    // ==========================================
    @PostMapping("/add")
    public ResponseEntity<?> addAgency(
            @RequestParam("agencyData") String agencyJson, 
            @RequestParam(value = "agencyLogo", required = false) MultipartFile logo,
            @RequestParam("adminId") Long adminId
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            adminAddAgenciesModel agency = mapper.readValue(agencyJson, adminAddAgenciesModel.class);

            // --- 🟢 SUPABASE S3 UPLOAD ---
            if (logo != null && !logo.isEmpty()) {
                // Folder ka naam 'agency-logos' rakha hai
                String publicImageUrl = storageService.uploadFile(logo, "agency-logos");
                agency.setAgencyLogo(publicImageUrl); // URL save hoga, file name nahi
            }

            agencyService.saveAgency(agency, adminId);
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
            @RequestParam(value = "adminId", required = false) Long adminId
            ){
        try {
            ObjectMapper mapper = new ObjectMapper();
            adminAddAgenciesModel updatedData = mapper.readValue(agencyJson, adminAddAgenciesModel.class);
            
            return agencyService.getAgencyById(id).map(agency -> {
                
                agency.setId(id);
                agency.setAgencyName(updatedData.getAgencyName());
                agency.setOwnerName(updatedData.getOwnerName());
                agency.setEmail(updatedData.getEmail());
                agency.setPhoneNumber(updatedData.getPhoneNumber());
                if (updatedData.getPassword() != null && !updatedData.getPassword().trim().isEmpty() && !updatedData.getPassword().equals("********")) {
                    agency.setPassword(updatedData.getPassword());
                }
                agency.setPlan(updatedData.getPlan());
                agency.setStatus(updatedData.getStatus());
                agency.setAddress(updatedData.getAddress());
                agency.setCountry(updatedData.getCountry());
                agency.setState(updatedData.getState());
                agency.setCity(updatedData.getCity());
                agency.setPincode(updatedData.getPincode());

                // --- 🟢 SUPABASE S3 LOGO UPDATE ---
                if (logo != null && !logo.isEmpty()) {
                    try {
                        // Naya logo upload karo aur naya URL lelo
                        // Note: S3 mein purani file delete karne ki zaroorat nahi hoti 
                        // kyunki hum unique names use kar rahe hain (UUID). 
                        String newPublicUrl = storageService.uploadFile(logo, "agency-logos");
                        agency.setAgencyLogo(newPublicUrl);
                    } catch (Exception e) {
                        System.err.println("❌ Logo Upload Failed: " + e.getMessage());
                    }
                }

                agencyService.saveAgency(agency, adminId);
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
    public ResponseEntity<List<adminAgenciesStatsdto>> getAgenciesList() {
        return ResponseEntity.ok(agencyService.getAllAgenciesForTable());
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
    public ResponseEntity<?> deleteAgency(@PathVariable Long id) {
        try {
            // Delete logic service se call karein
            agencyService.deleteAgency(id);
            return ResponseEntity.ok(Map.of("message", "Agency Deleted Successfully!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }
}