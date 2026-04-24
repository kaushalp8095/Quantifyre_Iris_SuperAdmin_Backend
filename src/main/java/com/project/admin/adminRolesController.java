package com.project.admin;

import com.project.common.models.adminRoleModel;
import com.project.common.service.AdminRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
@CrossOrigin(origins = "*")
public class adminRolesController {

    @Autowired
    private AdminRoleService roleService;

    // ==========================================
    // 1. LIST ROLES (type = ADMIN ya AGENCY)
    // ==========================================
    @GetMapping("/list")
    public ResponseEntity<List<adminRoleModel>> getRolesList(
            @RequestParam Long adminId,
            @RequestParam(defaultValue = "ADMIN") String type) {
        try {
            List<adminRoleModel> roles = roleService.getRolesByAdminAndType(adminId, type);
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // ==========================================
    // 2. GET ROLE DETAILS (Edit page ke liye)
    // ==========================================
    @GetMapping("/details/{id}")
    public ResponseEntity<?> getRoleDetails(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // 3. CREATE NEW ROLE
    // ==========================================
    @PostMapping("/add")
    public ResponseEntity<?> addRole(@RequestBody adminRoleModel role) {
        try {
            // adminId aur roleType frontend se aani chahiye
            if (role.getAdminId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "adminId required"));
            }
            if (role.getRoleType() == null || role.getRoleType().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "roleType required (ADMIN or AGENCY)"));
            }

            adminRoleModel saved = roleService.saveRole(role);
            return ResponseEntity.ok(Map.of(
                    "message", "Role Created Successfully!",
                    "id", saved.getId(),
                    "status", "success"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // 4. UPDATE ROLE
    // ==========================================
    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateRole(
            @PathVariable Long id,
            @RequestBody adminRoleModel updatedData) {
        try {
            return roleService.getRoleById(id).map(existingRole -> {
                existingRole.setId(id);
                existingRole.setRoleName(updatedData.getRoleName());
                existingRole.setDescription(updatedData.getDescription());
                existingRole.setPermissions(updatedData.getPermissions());
                // AGENCY role ke liye agencyId bhi update karo
                if (updatedData.getAgencyId() != null) {
                    existingRole.setAgencyId(updatedData.getAgencyId());
                }
                // roleType aur adminId change nahi hoga update me

                try {
                    roleService.saveRole(existingRole);
                    return ResponseEntity.ok(Map.of(
                            "message", "Role Updated Successfully!",
                            "status", "success"
                    ));
                } catch (RuntimeException e) {
                    return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
                }
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    // ==========================================
    // 5. DELETE ROLE
    // ==========================================
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Role Deleted Successfully!",
                    "status", "success"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }
}