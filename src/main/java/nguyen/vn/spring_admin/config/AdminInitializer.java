package nguyen.vn.spring_admin.config;

import jakarta.validation.Validator;
import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.form.UserForm;
import nguyen.vn.spring_admin.repository.UserRepository;
import nguyen.vn.spring_admin.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements ApplicationRunner {
    private final UserRepository repository;
    private final UserService service;
    private final Validator validator;
    private final boolean enabled;
    private final String username;
    private final String password;
    private final String email;

    public AdminInitializer(UserRepository repository, UserService service, Validator validator,
            @Value("${app.bootstrap.enabled:false}") boolean enabled,
            @Value("${app.bootstrap.username:admin}") String username,
            @Value("${app.bootstrap.password:}") String password,
            @Value("${app.bootstrap.email:admin@example.com}") String email) {
        this.repository = repository;
        this.service = service;
        this.validator = validator;
        this.enabled = enabled;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        if (password.isBlank()) {
            throw new IllegalStateException("app.bootstrap.password (APP_ADMIN_PASSWORD) is required when app.bootstrap.enabled=true.");
        }
        if (repository.existsByUsernameIgnoreCase(username.trim())) return;
        UserForm form = new UserForm();
        form.setUsername(username);
        form.setFullName("Quản trị viên");
        form.setEmail(email);
        form.setPassword(password);
        form.setRole(Role.ADMIN);
        form.setActive(true);
        var violations = validator.validate(form);
        if (!violations.isEmpty()) {
            String message = violations.stream().map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .sorted().reduce((first, second) -> first + "; " + second).orElse("");
            throw new IllegalStateException("Invalid bootstrap administrator configuration: " + message);
        }
        try {
            service.create(form);
        } catch (UserService.ValidationException ex) {
            throw new IllegalStateException("Invalid bootstrap administrator configuration: " + ex.getMessage(), ex);
        }
    }
}
