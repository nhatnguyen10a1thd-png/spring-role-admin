package nguyen.vn.spring_admin.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import nguyen.vn.spring_admin.entity.Category;

public class CategoryForm {

    @NotBlank(message = "Vui lòng nhập tên danh mục.")
    @Size(max = 120, message = "Tên danh mục không được vượt quá 120 ký tự.")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự.")
    private String description;

    private boolean active = true;

    public static CategoryForm from(Category category) {
        CategoryForm form = new CategoryForm();
        form.setName(category.getName());
        form.setDescription(category.getDescription());
        form.setActive(category.isActive());
        return form;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? null : description.strip();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
