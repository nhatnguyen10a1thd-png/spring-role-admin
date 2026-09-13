package nguyen.vn.spring_admin.controller.admin;

import jakarta.validation.Valid;
import nguyen.vn.spring_admin.form.CategoryForm;
import nguyen.vn.spring_admin.service.CategoryService;
import nguyen.vn.spring_admin.service.CategoryService.DuplicateCategoryException;

import org.springframework.dao.DataIntegrityViolationException;
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
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @InitBinder("categoryForm")
    void configureBinding(WebDataBinder binder) {
        binder.setAllowedFields("name", "description", "active");
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        model.addAttribute("pageData", service.search(keyword, page, size));
        model.addAttribute("keyword", keyword.strip());
        model.addAttribute("size", CategoryService.normalizeSize(size));
        return "admin/categories/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryForm", new CategoryForm());
        return prepareForm(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                service.create(form);
                redirect.addFlashAttribute("successMessage", "Đã thêm danh mục thành công.");
                return "redirect:/admin/categories";
            } catch (DuplicateCategoryException exception) {
                result.rejectValue("name", "duplicate", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                result.reject("saveFailed", "Không thể lưu danh mục. Tên danh mục có thể đã được sử dụng; vui lòng kiểm tra lại.");
            }
        }
        return prepareForm(model, null);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("category", service.findById(id));
        return "admin/categories/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("categoryForm", CategoryForm.from(service.findById(id)));
        return prepareForm(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("categoryForm") CategoryForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        // A malformed submission still must not show an edit screen for an absent row.
        service.findById(id);
        if (!result.hasErrors()) {
            try {
                service.update(id, form);
                redirect.addFlashAttribute("successMessage", "Đã cập nhật danh mục thành công.");
                return "redirect:/admin/categories";
            } catch (DuplicateCategoryException exception) {
                result.rejectValue("name", "duplicate", exception.getMessage());
            } catch (DataIntegrityViolationException exception) {
                result.reject("saveFailed", "Không thể lưu danh mục. Tên danh mục có thể đã được sử dụng; vui lòng kiểm tra lại.");
            }
        }
        return prepareForm(model, id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "10") int size,
                         RedirectAttributes redirect) {
        try {
            service.delete(id);
            redirect.addFlashAttribute("successMessage", "Đã xóa danh mục thành công.");
        } catch (DataIntegrityViolationException exception) {
            redirect.addFlashAttribute("errorMessage", "Không thể xóa danh mục đang được sử dụng.");
        }
        redirect.addAttribute("keyword", keyword.strip());
        redirect.addAttribute("page", Math.max(1, page));
        redirect.addAttribute("size", CategoryService.normalizeSize(size));
        return "redirect:/admin/categories";
    }

    private String prepareForm(Model model, Long id) {
        model.addAttribute("editMode", id != null);
        model.addAttribute("categoryId", id);
        return "admin/categories/form";
    }
}
