package app.dto;

public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private String token;

    public UserResponse(
            Long id,
            String name,
            String email,
            String role) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public UserResponse(
            Long id,
            String name,
            String email,
            String role,
            String token) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getToken() {
        return token;
    }
}