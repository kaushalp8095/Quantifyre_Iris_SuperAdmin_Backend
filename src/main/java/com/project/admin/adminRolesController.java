package com.project.admin;

import com.project.common.models.adminRoleModel;
import com.project.common.service.AdminActivityLogService;
import com.project.common.service.AdminRoleService;
import jakarta.servlet.http.HttpServletRequest;
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

    @Autowired
    private AdminActivityLogService logService; // ✅ LOG SERVICE

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) ip = request.getRemoteAddr();
        return ip;
    }

    // 1. LIST
    @GetMapping("/list")
    public ResponseEntity<List<adminRoleModel>> getRolesList(
            @RequestParam Long adminId,
            @RequestParam(defaultValue = "ADMIN") String type) {
        try {
            return ResponseEntity.ok(roleService.getRolesByAdminAndType(adminId, type));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // 2. DETAILS
    @GetMapping("/details/{id}")
    public ResponseEntity<?> getRoleDetails(@PathVariable Long id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 3. CREATE
    @PostMapping("/add")
    public ResponseEntity<?> addRole(@RequestBody adminRoleModel role,
                                      HttpServletRequest request) {
        try {
            if (role.getAdminId() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "adminId required"));
            if (role.getRoleType() == null || role.getRoleType().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "roleType required"));

            adminRoleModel saved = roleService.saveRole(role);

            // ✅ LOG
            logService.log(role.getAdminId(), "Super Admin", "ADMIN",
                    "Created Role: " + role.getRoleName() + " (" + role.getRoleType() + ")",
                    "Roles", getIp(request), "SUCCESS");

            return ResponseEntity.ok(Map.of(
                    "message", "Role Created Successfully!",
                    "id",      saved.getId(),
                    "status",  "success"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    // 4. UPDATE
    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateRole(@PathVariable Long id,
                                         @RequestBody adminRoleModel updatedData,
                                         HttpServletRequest request) {
        try {
            return roleService.getRoleById(id).map(existingRole -> {
                existingRole.setId(id);
                existingRole.setRoleName(updatedData.getRoleName());
                existingRole.setDescription(updatedData.getDescription());
                existingRole.setPermissions(updatedData.getPermissions());
                if (updatedData.getAgencyId() != null)
                    existingRole.setAgencyId(updatedData.getAgencyId());

                try {
                    roleService.saveRole(existingRole);

                    // ✅ LOG
                    logService.log(existingRole.getAdminId(), "Super Admin", "ADMIN",
                            "Updated Role: " + existingRole.getRoleName(),
                            "Roles", getIp(request), "SUCCESS");

                    return ResponseEntity.ok(Map.of("message", "Role Updated Successfully!", "status", "success"));
                } catch (RuntimeException e) {
                    return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
                }
            }).orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Server Error: " + e.getMessage()));
        }
    }

    // 5. DELETE
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable Long id,
                                         @RequestParam(required = false) Long adminId,
                                         HttpServletRequest request) {
        try {
            String roleName = roleService.getRoleById(id)
                    .map(r -> r.getRoleName()).orElse("ID: " + id);

            roleService.deleteRole(id);

            // ✅ LOG
            if (adminId != null) {
                logService.log(adminId, "Super Admin", "ADMIN",
                        "Deleted Role: " + roleName, "Roles", getIp(request), "SUCCESS");
            }

            return ResponseEntity.ok(Map.of("message", "Role Deleted Successfully!", "status", "success"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error: " + e.getMessage()));
        }
    }
}