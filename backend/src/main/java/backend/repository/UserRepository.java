package backend.repository;

import backend.entity.Role;
import backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByCreatedAtDesc();

    List<User> findByRoleOrderByCreatedAtDesc(Role role);

    long countByRole(Role role);

    long countByActive(boolean active);
}