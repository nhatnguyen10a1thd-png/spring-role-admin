package nguyen.vn.spring_admin.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import nguyen.vn.spring_admin.config.SecurityConfiguration.SessionUser;
import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.entity.User;
import nguyen.vn.spring_admin.form.UserForm;
import nguyen.vn.spring_admin.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class UserService {
    private static final Set<Integer> PAGE_SIZES = Set.of(5, 10, 20, 50);
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public long count() { return repository.count(); }

    public static int normalizeSize(int size) { return PAGE_SIZES.contains(size) ? size : 10; }

    public User findById(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng."));
    }

    public Page<User> search(String keyword, int page, int size) {
        int safeSize = normalizeSize(size);
        int safePage = Math.min(Math.max(1, page) - 1, Integer.MAX_VALUE / safeSize);
        String text = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + text.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_").replace("[", "![") + "%";
        Page<User> result = repository.search(pattern, PageRequest.of(safePage, safeSize, Sort.by("id").descending()));
        if (safePage > 0 && safePage >= result.getTotalPages()) {
            result = repository.search(pattern, PageRequest.of(Math.max(0, result.getTotalPages() - 1),
                    safeSize, Sort.by("id").descending()));
        }
        return result;
    }

    @Transactional
    public User create(UserForm form) {
        validateUnique(form, null);
        validatePassword(form.getPassword(), true);
        User user = new User();
        apply(user, form);
        return repository.saveAndFlush(user);
    }

    @Transactional
    public User update(Long id, UserForm form, Long actorId) {
        // Serialize removals of admin rights so two concurrent changes cannot remove every admin.
        List<User> admins = repository.lockActiveAdministrators(Role.ADMIN);
        User user = findById(id);
        validateUnique(form, id);
        validatePassword(form.getPassword(), false);
        if (Objects.equals(id, actorId)) {
            if (!form.isActive()) throw new ValidationException("active", "Bạn không thể khóa tài khoản đang đăng nhập.");
            if (form.getRole() != Role.ADMIN) throw new ValidationException("role", "Bạn không thể hạ quyền của tài khoản đang đăng nhập.");
        }
        if (user.isActive() && user.getRole() == Role.ADMIN && (!form.isActive() || form.getRole() != Role.ADMIN)
                && admins.size() <= 1) {
            throw new ValidationException(null, "Phải giữ ít nhất một quản trị viên đang hoạt động.");
        }
        apply(user, form);
        return repository.saveAndFlush(user);
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        List<User> admins = repository.lockActiveAdministrators(Role.ADMIN);
        User user = findById(id);
        if (Objects.equals(id, actorId)) {
            throw new ValidationException(null, "Bạn không thể xóa tài khoản đang đăng nhập.");
        }
        if (user.isActive() && user.getRole() == Role.ADMIN && admins.size() <= 1) {
            throw new ValidationException(null, "Không thể xóa quản trị viên đang hoạt động cuối cùng.");
        }
        repository.delete(user);
        repository.flush();
    }

    public Long actorId(Authentication authentication) {
        if (authentication == null) return null;
        if (authentication.getPrincipal() instanceof SessionUser principal) return principal.getId();
        return repository.findByUsernameIgnoreCase(authentication.getName()).map(User::getId).orElse(null);
    }

    private void validateUnique(UserForm form, Long id) {
        if (form.getRole() == null) throw new ValidationException("role", "Vui lòng chọn vai trò.");
        boolean usernameExists = id == null ? repository.existsByUsernameIgnoreCase(form.getUsername())
                : repository.existsByUsernameIgnoreCaseAndIdNot(form.getUsername(), id);
        if (usernameExists) throw new ValidationException("username", "Tên đăng nhập đã tồn tại.");
        boolean emailExists = id == null ? repository.existsByEmailIgnoreCase(form.getEmail())
                : repository.existsByEmailIgnoreCaseAndIdNot(form.getEmail(), id);
        if (emailExists) throw new ValidationException("email", "Email đã được sử dụng.");
    }

    private void validatePassword(String password, boolean required) {
        if (password == null || password.isEmpty()) {
            if (required) throw new ValidationException("password", "Vui lòng nhập mật khẩu cho tài khoản mới.");
            return;
        }
        if (password.isBlank() || password.length() < 8 || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ValidationException("password", "Mật khẩu phải có 8–72 ký tự và không quá 72 byte UTF-8.");
        }
    }

    private void apply(User user, UserForm form) {
        user.setUsername(form.getUsername().toLowerCase(Locale.ROOT));
        user.setFullName(form.getFullName());
        user.setEmail(form.getEmail().toLowerCase(Locale.ROOT));
        user.setRole(form.getRole());
        user.setActive(form.isActive());
        if (form.getPassword() != null && !form.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(form.getPassword()));
        }
    }

    public static class ValidationException extends RuntimeException {
        private final String field;
        public ValidationException(String field, String message) {
            super(message);
            this.field = field;
        }
        public String getField() { return field; }
    }
}
