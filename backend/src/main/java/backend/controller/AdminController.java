package backend.controller;

import backend.dto.AdminStatsResponse;
import backend.dto.AdminUserResponse;
import backend.dto.UpdateUserStatusRequest;
import backend.entity.Role;
import backend.security.AuthenticatedUser;
import backend.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> users(@RequestParam(required = false) Role role) {
        return adminService.listUsers(role);
    }

    @PatchMapping("/users/{userId}/status")
    public AdminUserResponse updateStatus(@AuthenticationPrincipal AuthenticatedUser admin,
                                          @PathVariable Long userId,
                                          @Valid @RequestBody UpdateUserStatusRequest request) {
        return adminService.setActive(admin.id(), userId, request.active());
    }
}