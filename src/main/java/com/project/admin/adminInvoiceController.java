package com.project.admin;

import com.project.common.models.adminInvoiceModel;
import com.project.common.service.AdminActivityLogService;
import com.project.common.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/invoices")
@CrossOrigin(origins = "*")
public class adminInvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private AdminActivityLogService logService; // ✅ LOG SERVICE

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip;
    }

    // 1. Dashboard Data
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam Long adminId) {
        return ResponseEntity.ok(invoiceService.getBillingDashboardData(adminId));
    }

    // 2. Create New Invoice
    @PostMapping("/add")
    public ResponseEntity<?> createInvoice(@RequestBody adminInvoiceModel invoice,
                                            HttpServletRequest request) {
        try {
            adminInvoiceModel saved = invoiceService.saveInvoice(invoice);

            // ✅ LOG
            logService.log(invoice.getAdminId(), "Super Admin", "ADMIN",
                    "Created Invoice #" + saved.getId(), "Billing", getIp(request), "SUCCESS");

            return ResponseEntity.ok(Map.of(
                    "message", "Invoice Created Successfully!",
                    "id",      saved.getId(),
                    "status",  "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Delete Invoice
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable Long id,
                                            @RequestParam(required = false) Long adminId,
                                            HttpServletRequest request) {
        try {
            invoiceService.deleteInvoice(id);

            // ✅ LOG
            if (adminId != null) {
                logService.log(adminId, "Super Admin", "ADMIN",
                        "Deleted Invoice #" + id, "Billing", getIp(request), "SUCCESS");
            }

            return ResponseEntity.ok(Map.of("message", "Deleted Successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 4. Get Invoice Details
    @GetMapping("/details/{id}")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
 // 5. Get Next Invoice Number
    @GetMapping("/next-no")
    public ResponseEntity<String> getNextInvoiceNo() {
        // Service ke generate function ko call kar rahe hain
        String nextNo = invoiceService.generateNextInvoiceNo(); 
        return ResponseEntity.ok(nextNo);
    }
}