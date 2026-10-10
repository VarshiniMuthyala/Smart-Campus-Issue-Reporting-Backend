package app.repository;

import app.model.Complaint;
import app.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByComplaint(Complaint complaint);

    Optional<Feedback> findByComplaintId(Long complaintId);

    boolean existsByComplaintId(Long complaintId);
}
