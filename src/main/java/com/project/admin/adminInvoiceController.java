package com.project.admin;

import com.project.common.models.adminInvoiceModel;
import com.project.common.service.InvoiceService;
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

    // 1. Dashboard Data (Stats + Table)
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam Long adminId) {
        return ResponseEntity.ok(invoiceService.getBillingDashboardData(adminId));
    }

    // 2. Create New Invoice
    @PostMapping("/add")
    public ResponseEntity<?> createInvoice(@RequestBody adminInvoiceModel invoice) {
        try {
            adminInvoiceModel saved = invoiceService.saveInvoice(invoice);
            return ResponseEntity.ok(Map.of(
                "message", "Invoice Created Successfully!",
                "id", saved.getId(),
                "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. Delete Invoice
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteInvoice(@PathVariable Long id) {
        try {
            invoiceService.deleteInvoice(id);
            return ResponseEntity.ok(Map.of("message", "Deleted Successfully!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
 // 4. Get Single Invoice Details (For View/Preview)
    @GetMapping("/details/{id}")
    public ResponseEntity<?> getInvoiceDetails(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}