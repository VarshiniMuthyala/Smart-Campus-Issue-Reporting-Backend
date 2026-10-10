package app.repository;

import app.model.Complaint;
import app.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TechnicianRepository
        extends JpaRepository<Technician, Long> {

    Optional<Technician> findByEmail(String email);

    boolean existsByEmail(String email);

    // Find technicians who are available and
    // have the required specialization
    List<Technician> findBySpecializationIgnoreCaseAndAvailableTrue(
            String specialization
    );

    // Count active complaints assigned to a technician
    @Query("""
        SELECT COUNT(c)
        FROM Complaint c
        WHERE c.technician.id = :technicianId
        AND c.status IN ('PENDING', 'IN_PROGRESS')
    """)
    long countActiveComplaints(
            @Param("technicianId") Long technicianId
    );
}