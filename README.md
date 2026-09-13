# Spring Admin — Category & User

Ứng dụng quản trị dùng **Spring Boot 4.1.1**, Java 17+, Spring MVC, Spring Data JPA,
Spring Security, **JSP/JSTL Jakarta**, **SiteMesh 3** và **Bootstrap 5.3.8**.
Đóng gói WAR để chạy với Tomcat nhúng hoặc triển khai lên Tomcat 11.

## Chạy thử nhanh

Yêu cầu JDK 17 trở lên; Maven Wrapper tự tải Maven và dependency ở lần chạy đầu.

```powershell
.\mvnw.cmd spring-boot:run
```

Truy cập **http://localhost:8090/login**.

| Tài khoản demo | Giá trị |
| --- | --- |
| Tên đăng nhập | `admin` |
| Mật khẩu | `Admin@123` |

Profile mặc định `demo` dùng H2 dạng file trong `data/spring-admin-demo` và tạo
admin nếu tên đăng nhập chưa tồn tại. Dữ liệu được giữ khi khởi động lại.
Mật khẩu trên chỉ dùng cho chạy thử. Đổi mật khẩu qua màn hình sửa người dùng;
đổi biến môi trường bootstrap sẽ không ghi đè tài khoản đã tồn tại.

## Chức năng

- Đăng nhập/đăng xuất, chỉ `ADMIN` được truy cập `/admin/**`.
- Trang tổng quan hiển thị số danh mục, người dùng và tài khoản hoạt động.
- Category: danh sách, chi tiết, thêm, sửa, xóa; tên, mô tả, trạng thái hoạt động.
- User: danh sách, chi tiết, thêm, sửa, xóa; tên đăng nhập, họ tên, email, mật khẩu,
  vai trò `ADMIN`/`USER`, trạng thái hoạt động.
- Tìm kiếm Category theo tên/mô tả; User theo tên đăng nhập/họ tên/email,
  không phân biệt hoa thường. Các ký tự `%`, `_`, `[` được tìm theo nghĩa literal.
- Phân trang ở cơ sở dữ liệu, chọn 5/10/20/50 dòng mỗi trang, giữ từ khóa khi
  chuyển trang. Tham số `page` bắt đầu từ 1; trang vượt giới hạn được điều chỉnh.
- Kiểm tra dữ liệu ở server, báo lỗi trong form; chặn trùng tên danh mục,
  tên đăng nhập và email. Trạng thái rỗng và lỗi 400/403/404 có trang hiển thị.
- Mật khẩu lưu bằng BCrypt. Để trống mật khẩu khi sửa để giữ mật khẩu hiện tại.
  Khi đặt mật khẩu mới: tối thiểu 8 ký tự, tối đa 72 byte UTF-8.
- Không cho tự xóa/khóa/hạ quyền tài khoản đang đăng nhập; giữ ít nhất một admin
  đang hoạt động. Phiên đăng nhập phản ánh thay đổi quyền, khóa/xóa tài khoản
  và thay đổi mật khẩu ở request tiếp theo.
- Xóa và đăng xuất bằng POST có CSRF; dữ liệu hiển thị được escape HTML.
- Giao diện tiếng Việt, responsive; decorator dùng chung sidebar/header/footer.
  CSS/JS Bootstrap được lưu trong dự án, không cần CDN khi sử dụng.

## Kết nối SQL Server

Tạo database trống trước (Hibernate tạo các bảng khi khởi động):

```sql
CREATE DATABASE spring_admin;
```

Thiết lập các biến môi trường trong PowerShell rồi chạy profile `sqlserver`:

```powershell
$env:SPRING_PROFILES_ACTIVE='sqlserver'
$env:DB_URL='jdbc:sqlserver://localhost:1433;databaseName=spring_admin;encrypt=true;trustServerCertificate=true'
$env:DB_USERNAME='sa'
$env:DB_PASSWORD='<mat-khau-SQL-Server>'
$env:APP_BOOTSTRAP_ENABLED='true'
$env:APP_ADMIN_USERNAME='admin'
$env:APP_ADMIN_PASSWORD='<mat-khau-admin-moi-toi-thieu-8-ky-tu>'
$env:APP_ADMIN_EMAIL='admin@example.com'
.\mvnw.cmd spring-boot:run
```

`trustServerCertificate=true` trong ví dụ dành cho SQL Server cục bộ có chứng chỉ
tự ký. Cấu hình mặc định dùng `false`; với server có chứng chỉ hợp lệ, giữ `false`.
Sau lần tạo admin đầu tiên có thể đặt `APP_BOOTSTRAP_ENABLED=false`.
`DB_DDL_AUTO` mặc định `update` để phục vụ bài thực hành; có thể dùng `validate`
khi đã quản lý schema bằng migration riêng.

Hai bảng độc lập vì yêu cầu chưa quy định quan hệ Category–User:

| Bảng | Cột chính |
| --- | --- |
| `categories` | `id`, `name`, `name_key` (unique), `description`, `active`, `created_at` |
| `app_users` | `id`, `username` (unique), `full_name`, `email` (unique), `password`, `role`, `active`, `created_at` |

`app_users` tránh xung đột với từ khóa `USER`. Tên/mô tả danh mục và họ tên dùng
kiểu Unicode trên SQL Server. `name_key` bảo đảm tên danh mục không trùng khi
chỉ khác hoa/thường; username/email được chuẩn hóa chữ thường.

## Kiểm thử và đóng gói

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
java -jar target/spring_admin-0.0.1-SNAPSHOT.war
```

Các kiểm thử dùng profile `test` và H2 trong bộ nhớ, tách khỏi database chạy thử.
Bộ kiểm thử bao gồm CRUD, validation/uniqueness, phân trang/tìm kiếm, bảo vệ admin
và HTTP trên Tomcat thật để kiểm tra đăng nhập, CSRF, JSP/JSTL và SiteMesh.

Để triển khai WAR lên Tomcat ngoài, dùng Tomcat 11 và cấu hình profile/database
tương tự. Các URL trong JSP dùng context path nên vẫn hoạt động khi ứng dụng
được triển khai dưới `/spring_admin-0.0.1-SNAPSHOT`.

## Cấu trúc

```text
src/main/java/nguyen/vn/spring_admin/
  config/                 Security, admin bootstrap, JSP/Tomcat
  controller/admin/       CategoryController, UserController
  entity/                 Category, User, Role
  form/                   DTO và validation của form
  repository/             JPA repository
  service/                CRUD, tìm kiếm, quy tắc nghiệp vụ
src/main/resources/
  application*.properties Profiles demo / sqlserver
  static/                 Bootstrap, CSS, JavaScript
src/main/webapp/WEB-INF/
  decorators/admin.jsp    SiteMesh decorator
  views/                  JSP admin, đăng nhập, lỗi, phân trang
```

## Ghi chú SiteMesh

Dự án dùng **SiteMesh 3.3.0-RC1** (bản release candidate), nhánh hỗ trợ Spring Boot 4
và Jakarta EE 11. Cấu hình `sitemesh.integration=view-resolver` xử lý decorator
trong lúc Spring MVC render JSP, phù hợp Tomcat 11. Mapping `/admin` và `/admin/*`
đến `/WEB-INF/decorators/admin.jsp` nằm trong `application.properties`.

Tham khảo: [SiteMesh compatibility](https://github.com/sitemesh/sitemesh3/blob/master/README.md),
[SiteMesh configuration](https://github.com/sitemesh/sitemesh3/blob/master/CONFIGURATION.md),
[Spring Boot WAR deployment](https://docs.spring.io/spring-boot/how-to/deployment/traditional-deployment.html).
