package nguyen.vn.spring_admin.controller.admin;

import jakarta.validation.Valid;
import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.entity.User;
import nguyen.vn.spring_admin.form.UserForm;
import nguyen.vn.spring_admin.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @InitBinder("userForm")
    void allowedFields(WebDataBinder binder) {
        binder.setAllowedFields("username", "fullName", "email", "password", "role", "active");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, Model model) {
        String trimmedKeyword = keyword.trim();
        Page<User> pageData = service.search(trimmedKeyword, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute("keyword", trimmedKeyword);
        model.addAttribute("size", pageData.getSize());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("userForm", new UserForm());
        return form(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") UserForm userForm, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (errors.hasErrors()) return form(model, null);
        try {
            service.create(userForm);
            redirect.addFlashAttribute("successMessage", "Đã thêm người dùng thành công.");
            return "redirect:/admin/users";
        } catch (UserService.ValidationException ex) {
            bindError(errors, ex);
        } catch (DataIntegrityViolationException ex) {
            errors.reject("user.duplicate", "Tên đăng nhập hoặc email đã tồn tại. Vui lòng kiểm tra lại.");
        }
        return form(model, null);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("managedUser", service.findById(id));
        return "admin/users/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("userForm", UserForm.from(service.findById(id)));
        return form(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("userForm") UserForm userForm,
            BindingResult errors, Authentication authentication, Model model, RedirectAttributes redirect) {
        service.findById(id);
        if (errors.hasErrors()) return form(model, id);
        try {
            service.update(id, userForm, service.actorId(authentication));
            redirect.addFlashAttribute("successMessage", "Đã cập nhật người dùng thành công.");
            return "redirect:/admin/users";
        } catch (UserService.ValidationException ex) {
            bindError(errors, ex);
        } catch (DataIntegrityViolationException ex) {
            errors.reject("user.duplicate", "Tên đăng nhập hoặc email đã tồn tại. Vui lòng kiểm tra lại.");
        }
        return form(model, id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size, RedirectAttributes redirect) {
        try {
            service.delete(id, service.actorId(authentication));
            redirect.addFlashAttribute("successMessage", "Đã xóa người dùng thành công.");
        } catch (UserService.ValidationException ex) {
            redirect.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (DataIntegrityViolationException ex) {
            redirect.addFlashAttribute("errorMessage", "Không thể xóa người dùng đang có dữ liệu liên quan.");
        }
        redirect.addAttribute("keyword", keyword.trim());
        redirect.addAttribute("page", Math.max(1, page));
        redirect.addAttribute("size", UserService.normalizeSize(size));
        return "redirect:/admin/users";
    }

    private String form(Model model, Long id) {
        model.addAttribute("editMode", id != null);
        model.addAttribute("userId", id);
        model.addAttribute("roles", Role.values());
        return "admin/users/form";
    }

    private void bindError(BindingResult errors, UserService.ValidationException ex) {
        if (ex.getField() == null) errors.reject("user.invalid", ex.getMessage());
        else errors.rejectValue(ex.getField(), "user.invalid", ex.getMessage());
    }
}
