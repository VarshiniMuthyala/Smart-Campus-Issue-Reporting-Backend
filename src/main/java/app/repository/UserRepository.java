package app.repository;

import app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRoleAndAccountStatus(
            String role,
            String accountStatus
    );

    List<User> findByRoleAndAccountStatus(
            String role,
            String accountStatus
    );
}