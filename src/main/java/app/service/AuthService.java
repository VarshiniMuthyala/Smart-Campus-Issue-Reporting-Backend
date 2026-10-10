package app.service;

import app.dto.ForgotPasswordRequest;
import app.dto.LoginRequest;
import app.dto.ResetPasswordRequest;
import app.dto.SignupRequest;
import app.model.PasswordResetToken;
import app.model.RoleRequest;
import app.model.Technician;
import app.model.User;
import app.repository.PasswordResetTokenRepository;
import app.repository.RoleRequestRepository;
import app.repository.TechnicianRepository;
import app.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRequestRepository roleRequestRepository;
    private final TechnicianRepository technicianRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordHash passwordHash;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            RoleRequestRepository roleRequestRepository,
            TechnicianRepository technicianRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordHash passwordHash,
            EmailService emailService) {

        this.userRepository = userRepository;
        this.roleRequestRepository = roleRequestRepository;
        this.technicianRepository = technicianRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordHash = passwordHash;
        this.emailService = emailService;
    }

    // ==========================================
    // SIGNUP
    // ==========================================

    public User signup(
            SignupRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Signup details are required."
            );
        }

        if (request.getName() == null ||
                request.getName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Name is required."
            );
        }

        if (request.getEmail() == null ||
                request.getEmail().trim().isEmpty()) {

            throw new RuntimeException(
                    "Email is required."
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().length() < 6) {

            throw new RuntimeException(
                    "Password must contain at least 6 characters."
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        String role =
                request.getRole();

        if (role == null ||
                role.trim().isEmpty()) {

            role = "STUDENT";
        }

        role =
                role.trim()
                        .toUpperCase();

        if (!role.equals("STUDENT") &&
            !role.equals("ADMIN") &&
            !role.equals("TECHNICIAN")) {

            throw new RuntimeException(
                    "Invalid role selected."
            );
        }

        // ======================================
        // EXISTING USER
        // ======================================

        if (userRepository.existsByEmail(email)) {

            throw new RuntimeException(
                    "Email already registered."
            );
        }

        // ======================================
        // STUDENT
        // ======================================

        if (role.equals("STUDENT")) {

            User user = new User();

            user.setName(
                    request.getName().trim()
            );

            user.setEmail(email);

            user.setPassword(
                    passwordHash.encode(
                            request.getPassword()
                    )
            );

            user.setRole("STUDENT");
            user.setAccountStatus("ACTIVE");

            return userRepository.save(user);
        }

        // ======================================
        // ADMIN
        // ======================================

        if (role.equals("ADMIN")) {

            boolean adminExists =
                    userRepository
                            .existsByRoleAndAccountStatus(
                                    "ADMIN",
                                    "ACTIVE"
                            );

            // ----------------------------------
            // FIRST ADMIN
            // ----------------------------------

            if (!adminExists) {

                User user = new User();

                user.setName(
                        request.getName().trim()
                );

                user.setEmail(email);

                user.setPassword(
                        passwordHash.encode(
                                request.getPassword()
                        )
                );

                user.setRole("ADMIN");
                user.setAccountStatus("ACTIVE");

                return userRepository.save(user);
            }

            // ----------------------------------
            // ADDITIONAL ADMIN
            // ----------------------------------

            createRoleRequest(
                    request,
                    "ADMIN"
            );

            throw new RuntimeException(
                    "Admin request submitted. Waiting for approval."
            );
        }

        // ======================================
        // TECHNICIAN
        // ======================================

        validateTechnicianSignup(request);

        createRoleRequest(
                request,
                "TECHNICIAN"
        );

        throw new RuntimeException(
                "Technician request submitted. Waiting for admin approval."
        );
    }

    // ==========================================
    // TECHNICIAN VALIDATION
    // ==========================================

    private void validateTechnicianSignup(
            SignupRequest request) {

        if (request.getDepartment() == null ||
                request.getDepartment()
                        .trim()
                        .isEmpty()) {

            throw new RuntimeException(
                    "Department is required for technicians."
            );
        }

        if (request.getPhone() == null ||
                !request.getPhone()
                        .matches("\\d{10}")) {

            throw new RuntimeException(
                    "Technician phone number must contain exactly 10 digits."
            );
        }

        if (request.getSpecialization() == null ||
                request.getSpecialization()
                        .trim()
                        .isEmpty()) {

            throw new RuntimeException(
                    "Technician specialization is required."
            );
        }

        String specialization =
                request.getSpecialization()
                        .trim()
                        .toUpperCase();

        if (!isValidSpecialization(
                specialization
        )) {

            throw new RuntimeException(
                    "Invalid technician specialization."
            );
        }
    }

    // ==========================================
    // VALID SPECIALIZATIONS
    // ==========================================

    private boolean isValidSpecialization(
            String specialization) {

        return specialization.equals("ELECTRICAL") ||
               specialization.equals("PLUMBING") ||
               specialization.equals("NETWORK") ||
               specialization.equals("FURNITURE") ||
               specialization.equals("CIVIL") ||
               specialization.equals("GENERAL") ||
               specialization.equals("GENERAL MAINTENANCE");
    }

    // ==========================================
    // CREATE ROLE REQUEST
    // ==========================================

    private void createRoleRequest(
            SignupRequest request,
            String role) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        if (roleRequestRepository
                .existsByEmailAndStatus(
                        email,
                        "PENDING"
                )) {

            throw new RuntimeException(
                    "A request with this email is already pending."
            );
        }

        RoleRequest roleRequest =
                new RoleRequest();

        roleRequest.setName(
                request.getName().trim()
        );

        roleRequest.setEmail(
                email
        );

        /*
         * Store the password using BCrypt.
         * It will be converted into a real User
         * only after admin approval.
         */
        roleRequest.setPassword(
                passwordHash.encode(
                        request.getPassword()
                )
        );

        roleRequest.setRequestedRole(
                role
        );

        roleRequest.setDepartment(
                request.getDepartment()
        );

        roleRequest.setPhone(
                request.getPhone()
        );

        roleRequest.setSpecialization(
                request.getSpecialization()
        );

        roleRequest.setStatus(
                "PENDING"
        );

        roleRequestRepository.save(
                roleRequest
        );
    }

    // ==========================================
    // LOGIN
    // ==========================================

    public User login(
            LoginRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Login details are required."
            );
        }

        if (request.getEmail() == null ||
                request.getEmail()
                        .trim()
                        .isEmpty()) {

            throw new RuntimeException(
                    "Email is required."
            );
        }

        if (request.getPassword() == null ||
                request.getPassword().isEmpty()) {

            throw new RuntimeException(
                    "Password is required."
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid email or password."
                                )
                        );

        // ======================================
        // ACCOUNT STATUS
        // ======================================

        if (!"ACTIVE".equalsIgnoreCase(
                user.getAccountStatus()
        )) {

            throw new RuntimeException(
                    "Your account is not active."
            );
        }

        // ======================================
        // PASSWORD CHECK
        // ======================================

        if (!passwordHash.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new RuntimeException(
                    "Invalid email or password."
            );
        }

        // ======================================
        // UPGRADE OLD PASSWORD HASH
        // ======================================

        if (!passwordHash.isBCryptHash(
                user.getPassword()
        )) {

            user.setPassword(
                    passwordHash.encode(
                            request.getPassword()
                    )
            );

            user = userRepository.save(user);
        }

        return user;
    }

    // ==========================================
    // GET ALL TECHNICIANS
    // ==========================================

    public List<Technician> getAllTechnicians() {

        return technicianRepository.findAll();
    }

    // ==========================================
    // GET PENDING ROLE REQUESTS
    // ==========================================

    public List<RoleRequest> getPendingRoleRequests() {

        return roleRequestRepository
                .findByStatus("PENDING");
    }

    // ==========================================
    // APPROVE ROLE REQUEST
    // ==========================================

    @Transactional
    public RoleRequest approveRoleRequest(
            Long requestId,
            String adminEmail) {

        verifyActiveAdmin(adminEmail);

        RoleRequest request =
                roleRequestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Role request not found."
                                )
                        );

        if (!"PENDING".equalsIgnoreCase(
                request.getStatus()
        )) {

            throw new RuntimeException(
                    "This request has already been processed."
            );
        }

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        if (userRepository.existsByEmail(email)) {

            throw new RuntimeException(
                    "A user with this email already exists."
            );
        }

        // ======================================
        // CREATE USER
        // ======================================

        User user = new User();

        user.setName(
                request.getName()
        );

        user.setEmail(
                email
        );

        user.setPassword(
                request.getPassword()
        );

        user.setRole(
                request.getRequestedRole()
                        .toUpperCase()
        );

        user.setAccountStatus(
                "ACTIVE"
        );

        User savedUser =
                userRepository.save(user);

        // ======================================
        // CREATE TECHNICIAN PROFILE
        // ======================================

        if ("TECHNICIAN".equalsIgnoreCase(
                request.getRequestedRole()
        )) {

            if (technicianRepository
                    .existsByEmail(email)) {

                throw new RuntimeException(
                        "Technician profile already exists."
                );
            }

            Technician technician =
                    new Technician();

            technician.setName(
                    request.getName()
            );

            technician.setEmail(
                    email
            );

            technician.setPhone(
                    request.getPhone()
            );

            technician.setDepartment(
                    request.getDepartment()
            );

            technician.setSpecialization(
                    request.getSpecialization()
                            .trim()
                            .toUpperCase()
            );

            technician.setAvailable(true);

            technicianRepository.save(
                    technician
            );
        }

        // ======================================
        // UPDATE REQUEST
        // ======================================

        request.setStatus("APPROVED");

        request.setApprovedBy(
                verifyActiveAdmin(adminEmail).getId()
        );

        RoleRequest savedRequest =
                roleRequestRepository.save(request);

        emailService.sendEmail(
                savedUser.getEmail(),
                "CampusFix - Registration Approved",
                "Hello " + savedUser.getName() + ",\n\n"
                        + "Your " + savedUser.getRole().toLowerCase()
                        + " registration request has been approved.\n\n"
                        + "You can now log in to CampusFix using your registered email and password.\n\n"
                        + "CampusFix Support Team"
        );

        return savedRequest;
    }

    // ==========================================
    // REJECT ROLE REQUEST
    // ==========================================

    @Transactional
    public RoleRequest rejectRoleRequest(
            Long requestId,
            String adminEmail) {

        User admin =
                verifyActiveAdmin(adminEmail);

        RoleRequest request =
                roleRequestRepository
                        .findById(requestId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Role request not found."
                                )
                        );

        if (!"PENDING".equalsIgnoreCase(
                request.getStatus()
        )) {

            throw new RuntimeException(
                    "This request has already been processed."
            );
        }

        request.setStatus("REJECTED");

        request.setApprovedBy(
                admin.getId()
        );

        RoleRequest savedRequest =
                roleRequestRepository.save(request);

        emailService.sendEmail(
                request.getEmail(),
                "CampusFix - Registration Request Update",
                "Hello " + request.getName() + ",\n\n"
                        + "Your " + request.getRequestedRole().toLowerCase()
                        + " registration request was not approved at this time.\n\n"
                        + "You may contact the campus administrator for more information.\n\n"
                        + "CampusFix Support Team"
        );

        return savedRequest;
    }

    // ==========================================
    // VERIFY ACTIVE ADMIN
    // ==========================================

    private User verifyActiveAdmin(
            String adminEmail) {

        if (adminEmail == null ||
                adminEmail.trim().isEmpty()) {

            throw new RuntimeException(
                    "Admin authentication is required."
            );
        }

        User admin =
                userRepository
                        .findByEmail(
                                adminEmail
                                        .trim()
                                        .toLowerCase()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Admin account not found."
                                )
                        );

        if (!"ADMIN".equalsIgnoreCase(
                admin.getRole()
        )) {

            throw new RuntimeException(
                    "Only an administrator can process role requests."
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(
                admin.getAccountStatus()
        )) {

            throw new RuntimeException(
                    "Administrator account is not active."
            );
        }

        return admin;
    }

    // ==========================================
    // FORGOT PASSWORD
    // ==========================================

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            return;
        }

        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {

            if (!"ACTIVE".equalsIgnoreCase(user.getAccountStatus())) {
                return;
            }

            // Invalidate any existing unused reset tokens for this user
            List<PasswordResetToken> existingTokens = passwordResetTokenRepository.findByUser(user);
            for (PasswordResetToken t : existingTokens) {
                t.setUsed(true);
            }
            passwordResetTokenRepository.saveAll(existingTokens);

            // Generate secure random reset token
            String token = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");

            LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(30);

            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
            passwordResetTokenRepository.save(resetToken);

            // Send password reset email
            String resetUrl = "http://localhost:5173/?resetToken=" + token;
            String emailSubject = "CampusFix - Password Reset Request";
            String emailBody = "Hello " + user.getName() + ",\n\n"
                    + "We received a request to reset your password for your CampusFix account.\n\n"
                    + "Click the link below to set a new password:\n"
                    + resetUrl + "\n\n"
                    + "This link will expire in 30 minutes and can only be used once.\n\n"
                    + "If you did not request a password reset, please ignore this email and your password will remain secure.\n\n"
                    + "CampusFix Support Team";

            emailService.sendEmail(user.getEmail(), emailSubject, emailBody);
        });
    }

    // ==========================================
    // VERIFY RESET TOKEN
    // ==========================================

    public boolean verifyResetToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        return passwordResetTokenRepository.findByToken(token.trim())
                .filter(t -> !t.isUsed() && !t.isExpired())
                .isPresent();
    }

    // ==========================================
    // RESET PASSWORD
    // ==========================================

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (request == null) {
            throw new RuntimeException("Reset password request cannot be empty.");
        }

        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new RuntimeException("Reset token is missing.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new RuntimeException("Password must contain at least 6 characters.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken().trim())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token."));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new RuntimeException("This reset link has expired or has already been used. Please request a new one.");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw new RuntimeException("User associated with this token not found.");
        }

        // Encode and update new password
        user.setPassword(passwordHash.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate token
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // Send confirmation email
        emailService.sendEmail(
                user.getEmail(),
                "CampusFix - Password Changed Successfully",
                "Hello " + user.getName() + ",\n\n"
                        + "Your CampusFix password has been successfully reset.\n\n"
                        + "You can now log in with your new password.\n\n"
                        + "If you did not perform this change, please contact support immediately.\n\n"
                        + "CampusFix Support Team"
        );
    }
}