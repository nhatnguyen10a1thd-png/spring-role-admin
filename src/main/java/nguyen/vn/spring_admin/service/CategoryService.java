package nguyen.vn.spring_admin.service;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import nguyen.vn.spring_admin.entity.Category;
import nguyen.vn.spring_admin.form.CategoryForm;
import nguyen.vn.spring_admin.repository.CategoryRepository;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final Set<Integer> PAGE_SIZES = Set.of(5, 10, 20, 50);
    private static final Sort LIST_SORT = Sort.by(Sort.Direction.DESC, "id");

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public Page<Category> search(String keyword, int page, int size) {
        String term = keyword == null ? "" : keyword.strip().toLowerCase(Locale.ROOT);
        // SQL Server also treats an opening bracket as a LIKE wildcard.
        // Escaping it is accepted by H2 as well as SQL Server.
        String pattern = "%" + term.replace("\\", "\\\\").replace("%", "\\%")
                .replace("_", "\\_").replace("[", "\\[") + "%";
        Specification<Category> specification = (root, query, builder) -> term.isEmpty()
                ? builder.conjunction()
                : builder.or(
                        builder.like(builder.lower(root.get("name")), pattern, '\\'),
                        builder.like(builder.lower(root.get("description")), pattern, '\\'));

        int pageSize = normalizeSize(size);
        // Clamp before executing the paginated query: an arbitrary client page
        // can otherwise exceed the JPA integer offset even for a tiny table.
        long totalElements = repository.count(specification);
        long lastAvailablePage = Math.max(0L, (totalElements - 1L) / pageSize);
        int pageIndex = (int) Math.min((long) Math.max(1, page) - 1L, lastAvailablePage);
        Page<Category> result = repository.findAll(specification, PageRequest.of(pageIndex, pageSize, LIST_SORT));
        // Re-clamp if rows were deleted between the initial count and the read.
        int lastPageIndex = Math.max(0, result.getTotalPages() - 1);
        if (pageIndex > lastPageIndex) {
            result = repository.findAll(specification, PageRequest.of(lastPageIndex, pageSize, LIST_SORT));
        }
        return result;
    }

    public static int normalizeSize(int size) {
        return PAGE_SIZES.contains(size) ? size : 10;
    }

    public Category findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy danh mục."));
    }

    @Transactional
    public Category create(CategoryForm form) {
        if (repository.existsByNameKey(normalizedName(form.getName()))) {
            throw new DuplicateCategoryException();
        }
        Category category = new Category();
        applyForm(category, form);
        return repository.saveAndFlush(category);
    }

    @Transactional
    public Category update(Long id, CategoryForm form) {
        Category category = findById(id);
        if (repository.existsByNameKeyAndIdNot(normalizedName(form.getName()), id)) {
            throw new DuplicateCategoryException();
        }
        applyForm(category, form);
        return repository.saveAndFlush(category);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
        repository.flush();
    }

    private static String normalizedName(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }

    private static void applyForm(Category category, CategoryForm form) {
        category.setName(form.getName());
        category.setDescription(form.getDescription());
        category.setActive(form.isActive());
    }

    public static class DuplicateCategoryException extends RuntimeException {
        public DuplicateCategoryException() {
            super("Tên danh mục đã tồn tại.");
        }
    }
}
