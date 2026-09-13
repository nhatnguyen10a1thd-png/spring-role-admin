package nguyen.vn.spring_admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import nguyen.vn.spring_admin.entity.Category;
import nguyen.vn.spring_admin.form.CategoryForm;
import nguyen.vn.spring_admin.repository.CategoryRepository;
import nguyen.vn.spring_admin.service.CategoryService;
import nguyen.vn.spring_admin.service.CategoryService.DuplicateCategoryException;

@SpringBootTest
@ActiveProfiles("test")
class CategoryServiceTests {

    @Autowired
    private CategoryService service;

    @Autowired
    private CategoryRepository repository;

    @Test
    void createsUpdatesAndDeletesCategory() {
        String prefix = uniquePrefix();
        Category created = service.create(form("  " + prefix + "Books  ", "Reading", true));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo(prefix + "Books");
        assertThat(created.getCreatedAt()).isNotNull();

        Category updated = service.update(created.getId(), form(prefix + "Novels", "Fiction", false));
        Category reloaded = service.findById(created.getId());
        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(reloaded.getName()).isEqualTo(prefix + "Novels");
        assertThat(reloaded.getDescription()).isEqualTo("Fiction");
        assertThat(reloaded.isActive()).isFalse();
        assertThat(reloaded.getCreatedAt()).isEqualToIgnoringNanos(created.getCreatedAt());

        service.delete(created.getId());
        assertThatThrownBy(() -> service.findById(created.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void rejectsNamesRegardlessOfCaseAndAllowsOwnUnchangedName() {
        String prefix = uniquePrefix();
        Category first = service.create(form(prefix + "Books", "", true));
        Category second = service.create(form(prefix + "Movies", "", true));
        try {
            assertThatThrownBy(() -> service.create(form("  " + prefix.toUpperCase() + "BOOKS  ", "", true)))
                    .isInstanceOf(DuplicateCategoryException.class);
            assertThatThrownBy(() -> service.update(second.getId(), form(prefix + "books", "", true)))
                    .isInstanceOf(DuplicateCategoryException.class);
            assertThat(service.update(first.getId(), form(prefix + "BOOKS", "Changed", true)).getDescription())
                    .isEqualTo("Changed");
            assertThat(service.findById(second.getId()).getName()).isEqualTo(prefix + "Movies");
        } finally {
            repository.deleteById(first.getId());
            repository.deleteById(second.getId());
        }
    }

    @Test
    void databaseRejectsDuplicateEvenWhenPrecheckIsBypassed() {
        String prefix = uniquePrefix();
        Category first = service.create(form(prefix + "Books", "", true));
        Category duplicate = new Category();
        duplicate.setName(prefix.toUpperCase() + "BOOKS");
        try {
            assertThatThrownBy(() -> repository.saveAndFlush(duplicate))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThat(service.findById(first.getId()).getName()).isEqualTo(prefix + "Books");
        } finally {
            repository.deleteById(first.getId());
        }
    }

    @Test
    void searchesNameDescriptionAndTreatsWildcardsAsLiteralCharacters() {
        String prefix = uniquePrefix();
        Category percent = service.create(form(prefix + "100%", "Special rate", true));
        Category underscore = service.create(form(prefix + "under_score", "", true));
        Category slash = service.create(form(prefix + "back\\slash", "", true));
        Category description = service.create(form(prefix + "Plain", prefix + "DescriptionToken", true));
        Category percentLookalike = service.create(form(prefix + "1000", "", true));
        Category underscoreLookalike = service.create(form(prefix + "underXscore", "", true));
        Category bracket = service.create(form(prefix + "[ab]", "", true));
        Category bracketLookalike = service.create(form(prefix + "a", "", true));
        try {
            assertThat(service.search(prefix + "100%", 1, 10).getContent())
                    .extracting(Category::getId).containsExactly(percent.getId());
            assertThat(service.search(prefix + "under_", 1, 10).getContent())
                    .extracting(Category::getId).containsExactly(underscore.getId());
            assertThat(service.search(prefix + "back\\", 1, 10).getContent())
                    .extracting(Category::getId).containsExactly(slash.getId());
            assertThat(service.search(prefix + "[ab]", 1, 10).getContent())
                    .extracting(Category::getId).containsExactly(bracket.getId());
            assertThat(service.search(prefix.toUpperCase() + "DESCRIPTIONTOKEN", 1, 10).getContent())
                    .extracting(Category::getId).containsExactly(description.getId());
        } finally {
            repository.deleteAllById(java.util.List.of(percent.getId(), underscore.getId(), slash.getId(),
                    description.getId(), percentLookalike.getId(), underscoreLookalike.getId(),
                    bracket.getId(), bracketLookalike.getId()));
        }
    }

    @Test
    void clampsPagesAndPageSizesAndHandlesEmptySearch() {
        String prefix = uniquePrefix();
        java.util.List<Long> ids = new java.util.ArrayList<>();
        try {
            for (int index = 0; index < 12; index++) {
                ids.add(service.create(form(prefix + index, "", true)).getId());
            }
            var first = service.search(prefix, -5, 5);
            assertThat(first.getNumber()).isZero();
            assertThat(first.getContent()).hasSize(5);
            assertThat(first.getTotalElements()).isEqualTo(12);
            assertThat(first.getTotalPages()).isEqualTo(3);

            var last = service.search(prefix, 999, 5);
            assertThat(last.getNumber()).isEqualTo(2);
            assertThat(last.getContent()).hasSize(2);
            assertThat(service.search(prefix, 1, 999).getSize()).isEqualTo(10);

            var maximumPage = service.search(prefix, Integer.MAX_VALUE, 5);
            assertThat(maximumPage.getNumber()).isEqualTo(2);
            assertThat(maximumPage.getContent()).hasSize(2);
            assertThat(service.search(prefix, Integer.MAX_VALUE, 50).getContent()).hasSize(12);
            assertThat(service.search(prefix, Integer.MIN_VALUE, 5).getNumber()).isZero();
            assertThat(service.search(prefix, 1, Integer.MAX_VALUE).getSize()).isEqualTo(10);
            assertThat(service.search(prefix, 1, Integer.MIN_VALUE).getSize()).isEqualTo(10);

            var empty = service.search(prefix + "absent", 999, 5);
            assertThat(empty.getNumber()).isZero();
            assertThat(empty.getContent()).isEmpty();
            var emptyAtMaximumPage = service.search(prefix + "absent", Integer.MAX_VALUE, 50);
            assertThat(emptyAtMaximumPage.getNumber()).isZero();
            assertThat(emptyAtMaximumPage.getContent()).isEmpty();
        } finally {
            repository.deleteAllById(ids);
        }
    }

    private static CategoryForm form(String name, String description, boolean active) {
        CategoryForm form = new CategoryForm();
        form.setName(name);
        form.setDescription(description);
        form.setActive(active);
        return form;
    }

    private static String uniquePrefix() {
        return "category-test-" + UUID.randomUUID() + "-";
    }
}
