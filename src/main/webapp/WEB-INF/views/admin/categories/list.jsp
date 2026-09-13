<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Danh mục</title></head><body>
<div class="page-heading"><div><h1>Danh mục</h1></div><a class="btn btn-primary" href="<c:url value='/admin/categories/new'/>"><span class="btn-plus" aria-hidden="true">＋</span> Thêm danh mục</a></div>
<%@ include file="../../fragments/flash.jsp" %>
<section class="panel" aria-labelledby="category-list-title">
    <div class="panel-header"><div><h2 id="category-list-title">Danh sách danh mục <span class="count-badge">${pageData.totalElements}</span></h2></div></div>
    <form method="get" action="<c:url value='/admin/categories'/>" class="filter-bar" role="search">
        <div class="search-fields"><div class="search-field"><label class="filter-label" for="keyword">Tìm kiếm danh mục</label><div class="search-input-wrap"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10" cy="10" r="6"/><path d="m15 15 5 5"/></svg><input class="form-control" id="keyword" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Nhập tên hoặc mô tả danh mục..." maxlength="200"></div></div><button type="submit" class="btn btn-primary">Tìm kiếm</button><c:if test="${not empty keyword}"><a class="btn btn-outline-secondary" href="<c:url value='/admin/categories'/>">Xóa lọc</a></c:if></div>
        <div class="filter-size"><label for="size" class="filter-label">Mỗi trang</label><select id="size" name="size" class="form-select size-select"><option value="5" ${size == 5 ? 'selected' : ''}>5 dòng</option><option value="10" ${size == 10 ? 'selected' : ''}>10 dòng</option><option value="20" ${size == 20 ? 'selected' : ''}>20 dòng</option><option value="50" ${size == 50 ? 'selected' : ''}>50 dòng</option></select></div>
    </form>
    <c:choose><c:when test="${pageData.hasContent()}"><div class="table-responsive"><table class="table"><caption class="visually-hidden">Danh sách danh mục và thao tác quản lý</caption><thead><tr><th scope="col">Danh mục</th><th scope="col">Mô tả</th><th scope="col">Trạng thái</th><th scope="col" class="text-end">Thao tác</th></tr></thead><tbody>
    <c:forEach var="category" items="${pageData.content}"><tr>
        <td><div class="cell-main"><span class="row-icon" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v10H3Z"/></svg></span><div><a class="cell-title" href="<c:url value='/admin/categories/${category.id}'/>"><c:out value="${category.name}"/></a><div class="cell-subtitle">ID: ${category.id}</div></div></div></td>
        <td><div class="cell-description"><c:out value="${empty category.description ? 'Chưa có mô tả' : category.description}"/></div></td>
        <td><span class="status-badge ${category.active ? '' : 'status-inactive'}">${category.active ? 'Hoạt động' : 'Tạm ẩn'}</span></td>
        <td><div class="row-actions"><a class="action-link" href="<c:url value='/admin/categories/${category.id}'/>" aria-label="Xem danh mục <c:out value='${category.name}'/>">Xem</a><a class="action-link" href="<c:url value='/admin/categories/${category.id}/edit'/>" aria-label="Sửa danh mục <c:out value='${category.name}'/>">Sửa</a><form method="post" action="<c:url value='/admin/categories/${category.id}/delete'/>" data-confirm-delete><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="keyword" value="<c:out value='${keyword}'/>"><input type="hidden" name="page" value="${pageData.number + 1}"><input type="hidden" name="size" value="${size}"><button type="submit" class="action-link action-delete" aria-label="Xóa danh mục <c:out value='${category.name}'/>">Xóa</button></form></div></td>
    </tr></c:forEach>
    </tbody></table></div></c:when><c:otherwise><div class="empty-state"><div class="empty-icon" aria-hidden="true">⌕</div><h3>${empty keyword ? 'Chưa có danh mục nào' : 'Không tìm thấy danh mục'}</h3><p>${empty keyword ? 'Tạo danh mục đầu tiên để bắt đầu tổ chức nội dung.' : 'Thử từ khóa khác hoặc xóa bộ lọc để xem tất cả danh mục.'}</p><c:choose><c:when test="${empty keyword}"><a class="btn btn-primary" href="<c:url value='/admin/categories/new'/>">Thêm danh mục đầu tiên</a></c:when><c:otherwise><a class="btn btn-outline-secondary" href="<c:url value='/admin/categories'/>">Xem tất cả danh mục</a></c:otherwise></c:choose></div></c:otherwise></c:choose>
    <c:set var="basePath" value="/admin/categories"/>
    <%@ include file="../../fragments/pagination.jsp" %>
</section>
</body></html>
