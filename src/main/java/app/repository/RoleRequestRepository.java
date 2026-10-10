package app.repository;

import app.model.RoleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRequestRepository
        extends JpaRepository<RoleRequest, Long> {

    List<RoleRequest> findByStatus(String status);

    Optional<RoleRequest> findByEmail(String email);

    boolean existsByEmailAndStatus(
            String email,
            String status
    );
}