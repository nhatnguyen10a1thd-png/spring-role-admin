package nguyen.vn.spring_admin.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.entity.User;

public class UserForm {
    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự.")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Tên đăng nhập chỉ gồm chữ, số, dấu chấm, gạch dưới và gạch ngang.")
    private String username;

    @NotBlank(message = "Vui lòng nhập họ và tên.")
    @Size(max = 120, message = "Họ và tên tối đa 120 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 254, message = "Email tối đa 254 ký tự.")
    private String email;

    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự và 72 byte UTF-8.")
    private String password = "";

    @NotNull(message = "Vui lòng chọn vai trò.")
    private Role role = Role.USER;

    private boolean active = true;

    public static UserForm from(User user) {
        UserForm form = new UserForm();
        form.setUsername(user.getUsername());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole());
        form.setActive(user.isActive());
        return form;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username == null ? null : username.trim(); }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName == null ? null : fullName.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password == null ? "" : password; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
