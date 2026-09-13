<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="theme-color" content="#12283c">
    <title><sitemesh:write property="title"/> · AdminSpace</title>
    <link rel="stylesheet" href="<c:url value='/static/vendor/bootstrap/bootstrap.min.css'/>">
    <link rel="stylesheet" href="<c:url value='/static/css/admin.css'/>">
    <sitemesh:write property="head"/>
</head>
<body class="admin-body">
<a class="skip-link" href="#main-content">Đến nội dung chính</a>
<div class="admin-shell">
    <aside class="sidebar offcanvas-lg offcanvas-start" tabindex="-1" id="adminSidebar" aria-label="Điều hướng quản trị">
        <a class="brand" href="<c:url value='/admin'/>"><span class="brand-mark" aria-hidden="true">a<span>■</span></span><span>Admin<span class="brand-light">Space</span></span></a>
        <button type="button" class="btn-close btn-close-white sidebar-close d-lg-none" data-bs-dismiss="offcanvas" data-bs-target="#adminSidebar" aria-label="Đóng điều hướng"></button>
        <div class="sidebar-label">MENU QUẢN TRỊ</div>
        <c:set var="currentUri" value="${not empty requestScope['jakarta.servlet.forward.request_uri'] ? requestScope['jakarta.servlet.forward.request_uri'] : pageContext.request.requestURI}"/>
        <nav class="sidebar-nav">
            <a href="<c:url value='/admin'/>" class="nav-entry ${not fn:contains(currentUri, '/categories') and not fn:contains(currentUri, '/users') ? 'is-active' : ''}">
                <svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></svg><span>Tổng quan</span>
            </a>
            <a href="<c:url value='/admin/categories'/>" class="nav-entry ${fn:contains(currentUri, '/categories') ? 'is-active' : ''}">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v10H3Z"/><path d="M3 10h18"/></svg><span>Danh mục</span>
            </a>
            <a href="<c:url value='/admin/users'/>" class="nav-entry ${fn:contains(currentUri, '/users') ? 'is-active' : ''}">
                <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 5a3 3 0 0 1 0 6M18 15a5 5 0 0 1 3 4v2"/></svg><span>Người dùng</span>
            </a>
        </nav>
    </aside>
    <div class="workspace">
        <header class="topbar">
            <div class="d-flex align-items-center gap-3"><button class="btn mobile-menu d-lg-none" type="button" data-bs-toggle="offcanvas" data-bs-target="#adminSidebar" aria-controls="adminSidebar" aria-label="Mở điều hướng">☰</button><div class="breadcrumb-trail"><strong><sitemesh:write property="title"/></strong></div></div>
            <div class="account-area"><div class="account-avatar" aria-hidden="true">A</div><div class="account-copy"><strong><c:out value="${pageContext.request.userPrincipal.name}"/></strong><span>Quản trị viên</span></div><form method="post" action="<c:url value='/logout'/>"><input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"><button class="btn btn-logout" type="submit" title="Đăng xuất" aria-label="Đăng xuất"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M9 4H4v16h5M14 8l4 4-4 4M8 12h12"/></svg></button></form></div>
        </header>
        <main id="main-content" class="main-content" tabindex="-1"><sitemesh:write property="body"/></main>
        <footer class="app-footer"><span>AdminSpace</span><span>Trang quản trị</span></footer>
    </div>
</div>
<script src="<c:url value='/static/vendor/bootstrap/bootstrap.bundle.min.js'/>"></script>
<script src="<c:url value='/static/js/admin.js'/>"></script>
</body>
</html>
