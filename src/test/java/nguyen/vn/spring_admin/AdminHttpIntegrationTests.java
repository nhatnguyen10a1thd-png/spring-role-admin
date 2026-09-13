package nguyen.vn.spring_admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nguyen.vn.spring_admin.entity.Category;
import nguyen.vn.spring_admin.entity.Role;
import nguyen.vn.spring_admin.entity.User;
import nguyen.vn.spring_admin.form.CategoryForm;
import nguyen.vn.spring_admin.form.UserForm;
import nguyen.vn.spring_admin.repository.CategoryRepository;
import nguyen.vn.spring_admin.repository.UserRepository;
import nguyen.vn.spring_admin.service.CategoryService;
import nguyen.vn.spring_admin.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.HtmlUtils;

/** Exercises JSP compilation, decorators and security through actual Tomcat HTTP requests. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:admin-http-test;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class AdminHttpIntegrationTests {
    private static final String TEST_PASSWORD = "HttpTest_2026!";
    private static final Pattern INPUT = Pattern.compile("<input\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*([\"'])(.*?)\\2", Pattern.DOTALL);
    private static final Pattern HREF = Pattern.compile("\\bhref\\s*=\\s*([\"'])(.*?)\\1", Pattern.CASE_INSENSITIVE);

    @Value("${local.server.port}")
    private int port;

    @Autowired private UserService userService;
    @Autowired private CategoryService categoryService;
    @Autowired private UserRepository users;
    @Autowired private CategoryRepository categories;
    @Autowired private PasswordEncoder passwordEncoder;

    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();
    private HttpClient browser;
    private User administrator;

    @BeforeEach
    void setUp() {
        browser = newBrowser();
        administrator = seedUser(Role.ADMIN, "Quản trị viên kiểm thử");
    }

    @AfterEach
    void cleanUp() {
        categories.deleteAllById(categoryIds);
        users.deleteAllById(userIds);
    }

    @Test
    void rendersLoginDashboardAssetsAndErrorViewsAndEnforcesAdminAccess() throws Exception {
        HttpResponse<String> anonymous = get(browser, "/admin/categories");
        assertRedirectTo(anonymous, "/login");

        HttpResponse<String> login = get(browser, "/login");
        assertHtml(login, 200);
        assertThat(inputValue(login.body(), "username")).isNotNull();
        HttpResponse<String> failedLogin = post(browser, "/login", Map.of(
                "username", administrator.getUsername(), "password", "incorrect-password", "_csrf", csrf(login)));
        assertRedirectTo(failedLogin, "/login");
        assertThat(query(failedLogin.headers().firstValue("Location").orElseThrow())).containsEntry("error", "");
        assertHtml(followRedirect(browser, failedLogin), 200);

        signIn(browser, administrator);
        HttpResponse<String> dashboard = get(browser, "/admin");
        assertDecorated(dashboard);
        assertThat(dashboard.body()).contains(administrator.getUsername());
        for (String asset : List.of("/static/css/admin.css", "/static/vendor/bootstrap/bootstrap.min.css")) {
            HttpResponse<String> response = get(browser, asset);
            assertThat(response.statusCode()).as(asset).isEqualTo(200);
            assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("text/css");
            assertThat(response.body()).isNotBlank();
        }

        long categoryCount = categories.count();
        HttpResponse<String> noCsrf = post(browser, "/admin/categories", Map.of("name", "Rejected without CSRF"));
        assertHtml(noCsrf, 403);
        assertThat(categories.count()).isEqualTo(categoryCount);
        assertHtml(get(browser, "/admin/categories?page=not-a-number"), 400);
        assertHtml(get(browser, "/admin/users?page=not-a-number"), 400);
        for (String missing : List.of("/admin/categories/9223372036854775807",
                "/admin/categories/9223372036854775807/edit", "/admin/users/9223372036854775807",
                "/admin/users/9223372036854775807/edit")) {
            assertHtml(get(browser, missing), 404);
        }

        User regularUser = seedUser(Role.USER, "Người dùng thông thường");
        HttpClient regularBrowser = newBrowser();
        signIn(regularBrowser, regularUser);
        assertHtml(get(regularBrowser, "/admin/categories"), 403);
        assertHtml(get(regularBrowser, "/admin/users"), 403);
        assertHtml(get(regularBrowser, "/access-denied"), 403);
    }

    @Test
    void performsCategoryCrudAndRendersValidationDuplicateAndEscapedValues() throws Exception {
        signIn(browser, administrator);
        HttpResponse<String> form = get(browser, "/admin/categories/new");
        assertDecorated(form);
        assertThat(inputValue(form.body(), "_active")).isNotNull();

        long before = categories.count();
        HttpResponse<String> invalid = post(browser, "/admin/categories", Map.of(
                "name", "  ", "description", "x".repeat(1001), "_csrf", csrf(form)));
        assertDecorated(invalid);
        assertThat(invalid.body()).contains("Vui lòng nhập tên danh mục.");
        assertThat(categories.count()).isEqualTo(before);

        String name = "Sách & <b>" + unique() + "</b>";
        String description = "Mô tả tiếng Việt <script>alert(1)</script>";
        HttpResponse<String> created = post(browser, "/admin/categories", Map.of(
                "name", name, "description", description, "active", "true", "_csrf", csrf(invalid)));
        assertRedirectTo(created, "/admin/categories");
        Category category = categories.findAll().stream().filter(row -> name.equals(row.getName())).findFirst().orElseThrow();
        categoryIds.add(category.getId());
        assertThat(category.getDescription()).isEqualTo(description);
        assertThat(category.isActive()).isTrue();
        assertThat(category.getCreatedAt()).isNotNull();

        HttpResponse<String> list = get(browser, "/admin/categories?keyword=" + encode(name));
        assertDecorated(list);
        assertThat(list.body()).contains("Sách &amp; &lt;b&gt;").doesNotContain(name);
        HttpResponse<String> detail = get(browser, "/admin/categories/" + category.getId());
        assertDecorated(detail);
        assertThat(detail.body()).contains("Mô tả tiếng Việt", "&lt;script&gt;")
                .doesNotContain("<script>alert(1)</script>");

        HttpResponse<String> edit = get(browser, "/admin/categories/" + category.getId() + "/edit");
        assertDecorated(edit);
        assertThat(inputValue(edit.body(), "name")).isEqualTo(name);
        String updatedName = "Danh mục đã sửa " + unique();
        HttpResponse<String> updated = post(browser, "/admin/categories/" + category.getId(), Map.of(
                "name", updatedName, "description", "Nội dung mới", "_active", "on", "_csrf", csrf(edit)));
        assertRedirectTo(updated, "/admin/categories");
        Category saved = categories.findById(category.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo(updatedName);
        assertThat(saved.getDescription()).isEqualTo("Nội dung mới");
        assertThat(saved.isActive()).isFalse();

        HttpResponse<String> duplicateForm = get(browser, "/admin/categories/new");
        HttpResponse<String> duplicate = post(browser, "/admin/categories", Map.of(
                "name", updatedName.toUpperCase(Locale.ROOT), "_csrf", csrf(duplicateForm)));
        assertDecorated(duplicate);
        assertThat(duplicate.body()).contains("Tên danh mục đã tồn tại.");
        assertThat(categories.count()).isEqualTo(before + 1);

        HttpResponse<String> deleted = post(browser, "/admin/categories/" + category.getId() + "/delete", Map.of(
                "keyword", updatedName, "page", "99", "size", "5", "_csrf", csrf(duplicate)));
        assertRedirectTo(deleted, "/admin/categories");
        assertThat(query(deleted.headers().firstValue("Location").orElseThrow()))
                .containsEntry("keyword", updatedName).containsEntry("size", "5");
        assertThat(categories.findById(category.getId())).isEmpty();
        assertDecorated(followRedirect(browser, deleted));
        assertHtml(get(browser, "/admin/categories/" + category.getId()), 404);
    }

    @Test
    void paginationPreservesLiteralUnicodeKeywordsAndPageSizeForBothTables() throws Exception {
        signIn(browser, administrator);
        String categoryKeyword = "Nhóm & \"sale\"%_[ab] " + unique();
        String userKeyword = "Đội & <QA>_%[ab] " + unique();
        for (int index = 0; index < 7; index++) {
            CategoryForm form = new CategoryForm();
            form.setName(categoryKeyword + " " + index);
            categoryIds.add(categoryService.create(form).getId());
            seedUser(Role.USER, userKeyword + " " + index);
        }

        for (Map.Entry<String, String> table : Map.of("/admin/categories", categoryKeyword,
                "/admin/users", userKeyword).entrySet()) {
            String path = table.getKey();
            String keyword = table.getValue();
            HttpResponse<String> first = get(browser, path + "?keyword=" + encode(keyword) + "&size=5&page=1");
            assertDecorated(first);
            assertThat(inputValue(first.body(), "keyword")).isEqualTo(keyword);
            assertThat(detailIds(first.body(), path)).hasSize(5);
            String nextLink = paginationLink(first.body(), path, 2, keyword, 5);
            HttpResponse<String> second = get(browser, nextLink);
            assertDecorated(second);
            assertThat(detailIds(second.body(), path)).hasSize(2).doesNotContainAnyElementsOf(detailIds(first.body(), path));
            paginationLink(second.body(), path, 1, keyword, 5);

            HttpResponse<String> beyondLast = get(browser, path + "?keyword=" + encode(keyword)
                    + "&size=5&page=" + Integer.MAX_VALUE);
            assertDecorated(beyondLast);
            assertThat(detailIds(beyondLast.body(), path)).containsExactlyElementsOf(detailIds(second.body(), path));
            HttpResponse<String> belowFirst = get(browser, path + "?keyword=" + encode(keyword)
                    + "&size=5&page=" + Integer.MIN_VALUE);
            assertDecorated(belowFirst);
            assertThat(detailIds(belowFirst.body(), path)).containsExactlyElementsOf(detailIds(first.body(), path));
        }
    }

    @Test
    void performsUserCrudPreservesBlankPasswordAndRejectsSelfDeletion() throws Exception {
        signIn(browser, administrator);
        HttpResponse<String> form = get(browser, "/admin/users/new");
        assertDecorated(form);
        long before = users.count();
        HttpResponse<String> invalid = post(browser, "/admin/users", Map.of(
                "username", "bad username", "fullName", "", "email", "invalid", "password", "short",
                "role", "USER", "_csrf", csrf(form)));
        assertDecorated(invalid);
        assertThat(invalid.body()).contains("Vui lòng nhập họ và tên.");
        assertThat(users.count()).isEqualTo(before);

        String username = "http_" + unique();
        String fullName = "Nguyễn & <b>Đỗ</b>";
        String email = username + "@example.test";
        HttpResponse<String> created = post(browser, "/admin/users", Map.of(
                "username", username, "fullName", fullName, "email", email, "password", TEST_PASSWORD,
                "role", "USER", "active", "true", "_csrf", csrf(invalid)));
        assertRedirectTo(created, "/admin/users");
        User createdUser = users.findByUsernameIgnoreCase(username).orElseThrow();
        userIds.add(createdUser.getId());
        String storedHash = createdUser.getPassword();
        assertThat(passwordEncoder.matches(TEST_PASSWORD, storedHash)).isTrue();
        assertThat(storedHash).isNotEqualTo(TEST_PASSWORD);

        HttpResponse<String> detail = get(browser, "/admin/users/" + createdUser.getId());
        assertDecorated(detail);
        assertThat(detail.body()).contains("Nguyễn &amp; &lt;b&gt;Đỗ&lt;/b&gt;")
                .doesNotContain(storedHash, TEST_PASSWORD, fullName);
        HttpResponse<String> edit = get(browser, "/admin/users/" + createdUser.getId() + "/edit");
        assertDecorated(edit);
        assertThat(inputValue(edit.body(), "username")).isEqualTo(username);
        assertThat(inputValue(edit.body(), "password")).isNullOrEmpty();
        assertThat(edit.body()).doesNotContain(storedHash, TEST_PASSWORD);

        HttpResponse<String> updated = post(browser, "/admin/users/" + createdUser.getId(), Map.of(
                "username", username, "fullName", "Người dùng đã cập nhật", "email", email, "password", "",
                "role", "USER", "_active", "on", "_csrf", csrf(edit)));
        assertRedirectTo(updated, "/admin/users");
        User saved = users.findById(createdUser.getId()).orElseThrow();
        assertThat(saved.getFullName()).isEqualTo("Người dùng đã cập nhật");
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getPassword()).isEqualTo(storedHash);

        HttpResponse<String> ownDetail = get(browser, "/admin/users/" + administrator.getId());
        assertDecorated(ownDetail);
        HttpResponse<String> selfDeletion = post(browser, "/admin/users/" + administrator.getId() + "/delete",
                Map.of("_csrf", csrf(ownDetail)));
        assertRedirectTo(selfDeletion, "/admin/users");
        assertThat(users.findById(administrator.getId())).isPresent();
        HttpResponse<String> blockedList = followRedirect(browser, selfDeletion);
        assertDecorated(blockedList);
        assertThat(blockedList.body()).contains("Bạn không thể xóa tài khoản đang đăng nhập.");

        HttpResponse<String> deleted = post(browser, "/admin/users/" + createdUser.getId() + "/delete",
                Map.of("_csrf", csrf(blockedList)));
        assertRedirectTo(deleted, "/admin/users");
        assertThat(users.findById(createdUser.getId())).isEmpty();
        assertDecorated(followRedirect(browser, deleted));
        assertHtml(get(browser, "/admin/users/" + createdUser.getId()), 404);
    }

    private User seedUser(Role role, String fullName) {
        UserForm form = new UserForm();
        form.setUsername("http_" + role.name().toLowerCase(Locale.ROOT) + "_" + unique());
        form.setFullName(fullName);
        form.setEmail(form.getUsername() + "@example.test");
        form.setPassword(TEST_PASSWORD);
        form.setRole(role);
        User user = userService.create(form);
        userIds.add(user.getId());
        return user;
    }

    private static HttpClient newBrowser() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();
    }

    private void signIn(HttpClient client, User user) throws Exception {
        HttpResponse<String> login = get(client, "/login");
        assertHtml(login, 200);
        HttpResponse<String> response = post(client, "/login", Map.of("username", user.getUsername(),
                "password", TEST_PASSWORD, "_csrf", csrf(login)));
        assertRedirectTo(response, "/admin");
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(resolve(path)).timeout(Duration.ofSeconds(40)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> post(HttpClient client, String path, Map<String, String> fields) throws Exception {
        String body = fields.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(resolve(path)).timeout(Duration.ofSeconds(40))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private URI resolve(String path) {
        URI base = URI.create("http://localhost:" + port);
        URI resolved = base.resolve(path);
        assertThat(resolved.getHost()).isEqualTo("localhost");
        assertThat(resolved.getPort()).isEqualTo(port);
        return resolved;
    }

    private HttpResponse<String> followRedirect(HttpClient client, HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(302);
        return get(client, response.headers().firstValue("Location").orElseThrow());
    }

    private static void assertRedirectTo(HttpResponse<String> response, String path) {
        assertThat(response.statusCode()).as("HTTP status for %s", response.uri()).isEqualTo(302);
        assertThat(URI.create(response.headers().firstValue("Location").orElseThrow()).getPath()).isEqualTo(path);
    }

    private static void assertHtml(HttpResponse<String> response, int status) {
        assertThat(response.statusCode()).as("HTTP status for %s", response.uri()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("").toLowerCase(Locale.ROOT))
                .contains("text/html", "charset=utf-8");
        assertThat(response.body()).contains("<html", "</html>").doesNotContain("<%@", "<sitemesh:write");
    }

    private static void assertDecorated(HttpResponse<String> response) {
        assertHtml(response, 200);
        assertThat(response.body()).contains("admin-shell", "AdminSpace", "Danh mục", "Người dùng");
    }

    private static String csrf(HttpResponse<String> response) {
        String token = inputValue(response.body(), "_csrf");
        assertThat(token).as("Rendered CSRF hidden input for %s", response.uri()).isNotBlank();
        return token;
    }

    private static String inputValue(String html, String name) {
        Matcher inputs = INPUT.matcher(html);
        while (inputs.find()) {
            Matcher attributes = ATTRIBUTE.matcher(inputs.group());
            Map<String, String> values = new LinkedHashMap<>();
            while (attributes.find()) {
                values.put(attributes.group(1).toLowerCase(Locale.ROOT), HtmlUtils.htmlUnescape(attributes.group(3)));
            }
            if (name.equals(values.get("name"))) return values.getOrDefault("value", "");
        }
        return null;
    }

    private static List<String> links(String html) {
        List<String> links = new ArrayList<>();
        Matcher matcher = HREF.matcher(html);
        while (matcher.find()) links.add(HtmlUtils.htmlUnescape(matcher.group(2)));
        return links;
    }

    private static List<Long> detailIds(String html, String path) {
        return links(html).stream().map(URI::create).map(URI::getPath)
                .filter(value -> value != null && value.matches(Pattern.quote(path) + "/[0-9]+"))
                .map(value -> Long.valueOf(value.substring(value.lastIndexOf('/') + 1))).distinct().toList();
    }

    private static String paginationLink(String html, String path, int page, String keyword, int size) {
        String link = links(html).stream().filter(value -> path.equals(URI.create(value).getPath()))
                .filter(value -> Integer.toString(page).equals(query(value).get("page"))).findFirst().orElseThrow();
        assertThat(query(link)).containsEntry("keyword", keyword).containsEntry("size", Integer.toString(size));
        return link;
    }

    private static Map<String, String> query(String address) {
        Map<String, String> result = new LinkedHashMap<>();
        String rawQuery = URI.create(address).getRawQuery();
        if (rawQuery == null) return result;
        for (String parameter : rawQuery.split("&")) {
            String[] pair = parameter.split("=", 2);
            result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                    pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
        }
        return result;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String unique() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
