package backend.service;

import backend.dto.AdminStatsResponse;
import backend.dto.AdminUserResponse;
import backend.entity.PrescriptionStatus;
import backend.entity.Role;
import backend.entity.User;
import backend.repository.PrescriptionRepository;
import backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final PrescriptionRepository prescriptionRepository;

    public AdminService(UserRepository userRepository,
                        PrescriptionRepository prescriptionRepository) {
        this.userRepository = userRepository;
        this.prescriptionRepository = prescriptionRepository;
    }

    // Only totals are returned. No patient or prescription details are exposed to admins.
    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.ADMIN),
                userRepository.countByRole(Role.DOCTOR),
                userRepository.countByRole(Role.PATIENT),
                userRepository.countByActive(true),
                userRepository.countByActive(false),
                prescriptionRepository.count(),
                prescriptionRepository.countByStatus(PrescriptionStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(Role role) {
        List<User> users = (role == null)
                ? userRepository.findAllByOrderByCreatedAtDesc()
                : userRepository.findByRoleOrderByCreatedAtDesc(role);

        return users.stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserResponse setActive(Long adminUserId, Long targetUserId, boolean active) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (target.getId().equals(adminUserId) && !active) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "You cannot disable your own account");
        }

        target.setActive(active);
        userRepository.save(target);
        return toResponse(target);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt());
    }
}