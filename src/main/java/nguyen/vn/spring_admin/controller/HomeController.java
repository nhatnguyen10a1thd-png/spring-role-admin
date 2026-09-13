package nguyen.vn.spring_admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import nguyen.vn.spring_admin.repository.CategoryRepository;
import nguyen.vn.spring_admin.repository.UserRepository;

@Controller
public class HomeController {

    private final CategoryRepository categories;
    private final UserRepository users;

    public HomeController(CategoryRepository categories, UserRepository users) {
        this.categories = categories;
        this.users = users;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        return isAdmin(authentication) ? "redirect:/admin" : "redirect:/access-denied";
    }

    @GetMapping({"/admin", "/admin/"})
    public String dashboard(Model model) {
        model.addAttribute("categoryCount", categories.count());
        model.addAttribute("userCount", users.count());
        model.addAttribute("activeUserCount", users.countByActiveTrue());
        return "admin/dashboard";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return isAdmin(authentication) ? "redirect:/admin" : "redirect:/access-denied";
        }
        return "auth/login";
    }

    @RequestMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied() {
        return "error/403";
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
