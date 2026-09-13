package nguyen.vn.spring_admin;

import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.entity.User;
import nguyen.vn.spring_admin.form.UserForm;
import nguyen.vn.spring_admin.repository.UserRepository;
import nguyen.vn.spring_admin.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTests {
    @Autowired private UserService service;
    @Autowired private UserRepository repository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUsers() {
        repository.deleteAll();
        repository.flush();
    }

    @Test
    void createsNormalizedAccountWithHashedPassword() {
        User user = service.create(form("New.User", "NEW.USER@EXAMPLE.COM", Role.USER));
        assertEquals("new.user", user.getUsername());
        assertEquals("new.user@example.com", user.getEmail());
        assertNotEquals("Password@123", user.getPassword());
        assertTrue(passwordEncoder.matches("Password@123", user.getPassword()));
        assertNotNull(user.getCreatedAt());
    }

    @Test
    void emptyEditPasswordPreservesHashAndNeverAppearsInForm() {
        User user = service.create(form("edit.user", "edit@example.com", Role.USER));
        String originalHash = user.getPassword();
        UserForm edit = UserForm.from(user);
        assertEquals("", edit.getPassword());
        edit.setFullName("Đã cập nhật");
        User updated = service.update(user.getId(), edit, null);
        assertEquals(originalHash, updated.getPassword());
        assertEquals("Đã cập nhật", updated.getFullName());
    }

    @Test
    void createRequiresPassword() {
        UserForm form = form("missing.password", "missing@example.com", Role.USER);
        form.setPassword("");
        UserService.ValidationException failure = assertThrows(UserService.ValidationException.class, () -> service.create(form));
        assertEquals("password", failure.getField());
        assertFalse(repository.existsByUsernameIgnoreCase("missing.password"));
    }

    @Test
    void rejectsPasswordThatExceedsBcryptByteLimit() {
        UserForm form = form("long.password", "long@example.com", Role.USER);
        form.setPassword("á".repeat(37));
        UserService.ValidationException failure = assertThrows(UserService.ValidationException.class, () -> service.create(form));
        assertEquals("password", failure.getField());
    }

    @Test
    void rejectsUsernameDuplicatesIgnoringCase() {
        service.create(form("duplicate", "first@example.com", Role.USER));
        UserService.ValidationException failure = assertThrows(UserService.ValidationException.class,
                () -> service.create(form("DUPLICATE", "second@example.com", Role.USER)));
        assertEquals("username", failure.getField());
    }

    @Test
    void rejectsEmailDuplicatesIgnoringCase() {
        service.create(form("first.user", "existing@example.com", Role.USER));
        UserService.ValidationException failure = assertThrows(UserService.ValidationException.class,
                () -> service.create(form("second.user", "EXISTING@EXAMPLE.COM", Role.USER)));
        assertEquals("email", failure.getField());
    }

    @Test
    void administratorCannotDeleteOwnAccount() {
        User admin = service.create(form("self.admin", "self@example.com", Role.ADMIN));
        service.create(form("other.admin", "other@example.com", Role.ADMIN));
        assertThrows(UserService.ValidationException.class, () -> service.delete(admin.getId(), admin.getId()));
        assertTrue(repository.existsById(admin.getId()));
    }

    @Test
    void administratorCannotDisableOwnAccount() {
        User admin = service.create(form("disable.admin", "disable@example.com", Role.ADMIN));
        service.create(form("other.admin", "other@example.com", Role.ADMIN));
        UserForm edit = UserForm.from(admin);
        edit.setActive(false);
        UserService.ValidationException failure = assertThrows(UserService.ValidationException.class,
                () -> service.update(admin.getId(), edit, admin.getId()));
        assertEquals("active", failure.getField());
        assertTrue(service.findById(admin.getId()).isActive());
    }

    @Test
    void administratorCannotDemoteOwnAccount() {
        User admin = service.create(form("demote.admin", "demote@example.com", Role.ADMIN));
        service.create(form("other.admin", "other@example.com", Role.ADMIN));
        UserForm edit = UserForm.from(admin);
        edit.setRole(Role.USER);
        UserService.ValidationException failure = assertThrows(UserService.ValidationException.class,
                () -> service.update(admin.getId(), edit, admin.getId()));
        assertEquals("role", failure.getField());
        assertEquals(Role.ADMIN, service.findById(admin.getId()).getRole());
    }

    @Test
    void lastActiveAdministratorCannotBeDeleted() {
        User admin = service.create(form("last.admin", "last@example.com", Role.ADMIN));
        assertThrows(UserService.ValidationException.class, () -> service.delete(admin.getId(), null));
    }

    @Test
    void lastActiveAdministratorCannotBeDemoted() {
        User admin = service.create(form("last.admin", "last@example.com", Role.ADMIN));
        UserForm edit = UserForm.from(admin);
        edit.setRole(Role.USER);
        assertThrows(UserService.ValidationException.class, () -> service.update(admin.getId(), edit, null));
    }

    @Test
    void deletesOrdinaryUser() {
        User user = service.create(form("delete.user", "delete@example.com", Role.USER));
        service.delete(user.getId(), null);
        assertFalse(repository.existsById(user.getId()));
    }

    @Test
    void searchTreatsWildcardsLiterallyAndClampsPagination() {
        UserForm literal = form("literal_user", "literal@example.com", Role.USER);
        literal.setFullName("Giảm 100% [VIP] !");
        service.create(literal);
        service.create(form("literalXuser", "other@example.com", Role.USER));
        assertEquals(1, service.search("_", 1, 10).getTotalElements());
        assertEquals(1, service.search("%", 1, 10).getTotalElements());
        assertEquals(1, service.search("[VIP]", 1, 10).getTotalElements());
        assertEquals(1, service.search("!", 1, 10).getTotalElements());
        assertEquals(1, service.search("LITERAL@EXAMPLE.COM", 1, 10).getTotalElements());
        var page = service.search("", Integer.MAX_VALUE, 999);
        assertEquals(0, page.getNumber());
        assertEquals(10, page.getSize());
        assertEquals(2, page.getTotalElements());
    }

    private UserForm form(String username, String email, Role role) {
        UserForm form = new UserForm();
        form.setUsername(username);
        form.setFullName("Người dùng kiểm thử");
        form.setEmail(email);
        form.setPassword("Password@123");
        form.setRole(role);
        form.setActive(true);
        return form;
    }
}
