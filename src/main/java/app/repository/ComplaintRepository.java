package app.repository;

import app.model.Complaint;
import app.model.Technician;
import app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository
        extends JpaRepository<Complaint, Long> {

    List<Complaint> findByUser(User user);

    List<Complaint> findByStatus(String status);

    List<Complaint> findByCategory(String category);

    List<Complaint> findByTechnician(Technician technician);

    long countByStatus(String status);

    long countByCategory(String category);
}