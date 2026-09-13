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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserSecurityTests {
    @Autowired private MockMvc mvc;
    @Autowired private UserService service;
    @Autowired private UserRepository repository;
    private User managedUser;
    private User otherAdmin;

    @BeforeEach
    void setUpAccounts() {
        repository.deleteAll();
        repository.flush();
        managedUser = createAdmin("session.admin");
        otherAdmin = createAdmin("other.admin");
    }

    @Test
    void demotionRemovesAdminAccessFromExistingSession() throws Exception {
        MockHttpSession session = login();
        UserForm edit = UserForm.from(managedUser);
        edit.setRole(Role.USER);
        service.update(managedUser.getId(), edit, otherAdmin.getId());

        mvc.perform(get("/admin/users").session(session))
                .andExpect(status().isForbidden())
                .andExpect(authenticated().withRoles("USER"));
    }

    @Test
    void disablingAnAccountInvalidatesItsExistingSession() throws Exception {
        MockHttpSession session = login();
        UserForm edit = UserForm.from(managedUser);
        edit.setActive(false);
        service.update(managedUser.getId(), edit, otherAdmin.getId());

        expectRevokedSession(session);
    }

    @Test
    void deletingAnAccountInvalidatesItsExistingSession() throws Exception {
        MockHttpSession session = login();
        service.delete(managedUser.getId(), otherAdmin.getId());

        expectRevokedSession(session);
    }

    @Test
    void changingPasswordInvalidatesItsExistingSession() throws Exception {
        MockHttpSession session = login();
        UserForm edit = UserForm.from(managedUser);
        edit.setPassword("Changed@123");
        service.update(managedUser.getId(), edit, otherAdmin.getId());

        expectRevokedSession(session);
    }

    @Test
    void usernameChangePreservesTheAccountAndUpdatesPrincipal() throws Exception {
        MockHttpSession session = login();
        UserForm edit = UserForm.from(managedUser);
        edit.setUsername("renamed.admin");
        service.update(managedUser.getId(), edit, managedUser.getId());

        mvc.perform(get("/admin/users").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("renamed.admin").withRoles("ADMIN"));
        mvc.perform(post("/admin/users/{id}/delete", managedUser.getId()).session(session).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("errorMessage", "Bạn không thể xóa tài khoản đang đăng nhập."));
        assertTrue(repository.existsById(managedUser.getId()));
    }

    private void expectRevokedSession(MockHttpSession session) throws Exception {
        mvc.perform(get("/admin/users").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?expired"))
                .andExpect(unauthenticated());
        assertTrue(session.isInvalid());
    }

    private MockHttpSession login() throws Exception {
        var result = mvc.perform(post("/login").with(csrf())
                        .param("username", "session.admin").param("password", "Password@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername("session.admin"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private User createAdmin(String username) {
        UserForm form = new UserForm();
        form.setUsername(username);
        form.setFullName("Quản trị viên kiểm thử");
        form.setEmail(username + "@example.com");
        form.setPassword("Password@123");
        form.setRole(Role.ADMIN);
        return service.create(form);
    }
}
