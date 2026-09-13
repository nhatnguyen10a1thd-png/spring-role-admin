<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html><html lang="vi"><head><title>Người dùng</title></head><body>
<div class="page-heading"><div><h1>Người dùng</h1></div><a class="btn btn-primary" href="<c:url value='/admin/users/new'/>"><span class="btn-plus" aria-hidden="true">＋</span> Thêm người dùng</a></div>
<%@ include file="../../fragments/flash.jsp" %>
<section class="panel" aria-labelledby="user-list-title">
    <div class="panel-header"><div><h2 id="user-list-title">Danh sách người dùng <span class="count-badge">${pageData.totalElements}</span></h2></div></div>
    <form method="get" action="<c:url value='/admin/users'/>" class="filter-bar" role="search">
        <div class="search-fields"><div class="search-field"><label class="filter-label" for="keyword">Tìm kiếm người dùng</label><div class="search-input-wrap"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10" cy="10" r="6"/><path d="m15 15 5 5"/></svg><input class="form-control" id="keyword" name="keyword" value="<c:out value='${keyword}'/>" placeholder="Tên, tài khoản hoặc email..." maxlength="200"></div></div><button type="submit" class="btn btn-primary">Tìm kiếm</button><c:if test="${not empty keyword}"><a class="btn btn-outline-secondary" href="<c:url value='/admin/users'/>">Xóa lọc</a></c:if></div>
        <div class="filter-size"><label for="size" class="filter-label">Mỗi trang</label><select id="size" name="size" class="form-select size-select"><option value="5" ${size == 5 ? 'selected' : ''}>5 dòng</option><option value="10" ${size == 10 ? 'selected' : ''}>10 dòng</option><option value="20" ${size == 20 ? 'selected' : ''}>20 dòng</option><option value="50" ${size == 50 ? 'selected' : ''}>50 dòng</option></select></div>
    </form>
    <c:choose><c:when test="${pageData.hasContent()}"><div class="table-responsive"><table class="table"><caption class="visually-hidden">Danh sách tài khoản người dùng và quyền truy cập</caption><thead><tr><th scope="col">Người dùng</th><th scope="col">Email</th><th scope="col">Vai trò</th><th scope="col">Trạng thái</th><th scope="col" class="text-end">Thao tác</th></tr></thead><tbody>
    <c:forEach var="managedUser" items="${pageData.content}"><tr>
        <td><div class="cell-main"><span class="row-icon row-avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(managedUser.fullName, 0, 1))}"/></span><div><a class="cell-title" href="<c:url value='/admin/users/${managedUser.id}'/>"><c:out value="${managedUser.fullName}"/></a><div class="cell-subtitle">@<c:out value="${managedUser.username}"/></div></div></div></td>
        <td class="text-muted-custom"><c:out value="${managedUser.email}"/></td>
        <td><span class="role-badge ${managedUser.role == 'ADMIN' ? 'role-admin' : ''}">${managedUser.role == 'ADMIN' ? 'Quản trị viên' : 'Người dùng'}</span></td>
        <td><span class="status-badge ${managedUser.active ? '' : 'status-inactive'}">${managedUser.active ? 'Hoạt động' : 'Đã khóa'}</span></td>
        <td><div class="row-actions"><a class="action-link" href="<c:url value='/admin/users/${managedUser.id}'/>" aria-label="Xem người dùng <c:out value='${managedUser.username}'/>">Xem</a><a class="action-link" href="<c:url value='/admin/users/${managedUser.id}/edit'/>" aria-label="Sửa người dùng <c:out value='${managedUser.username}'/>">Sửa</a><form method="post" action="<c:url value='/admin/users/${managedUser.id}/delete'/>" data-confirm-delete><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><input type="hidden" name="keyword" value="<c:out value='${keyword}'/>"><input type="hidden" name="page" value="${pageData.number + 1}"><input type="hidden" name="size" value="${size}"><button type="submit" class="action-link action-delete" aria-label="Xóa người dùng <c:out value='${managedUser.username}'/>">Xóa</button></form></div></td>
    </tr></c:forEach>
    </tbody></table></div></c:when><c:otherwise><div class="empty-state"><div class="empty-icon" aria-hidden="true">⌕</div><h3>${empty keyword ? 'Chưa có người dùng nào' : 'Không tìm thấy người dùng'}</h3><p>${empty keyword ? 'Tạo tài khoản để thành viên có thể truy cập hệ thống.' : 'Thử tên, tài khoản hoặc email khác để tìm kiếm.'}</p><c:choose><c:when test="${empty keyword}"><a class="btn btn-primary" href="<c:url value='/admin/users/new'/>">Thêm người dùng</a></c:when><c:otherwise><a class="btn btn-outline-secondary" href="<c:url value='/admin/users'/>">Xem tất cả người dùng</a></c:otherwise></c:choose></div></c:otherwise></c:choose>
    <c:set var="basePath" value="/admin/users"/>
    <%@ include file="../../fragments/pagination.jsp" %>
</section>
</body></html>
