<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html><html lang="vi"><head><title>Tổng quan</title></head><body>
<div class="page-heading"><div><h1>Tổng quan</h1></div></div>
<%@ include file="../fragments/flash.jsp" %>
<section class="row g-4 mb-4" aria-label="Thống kê hệ thống">
    <div class="col-md-4"><a class="stat-card" href="<c:url value='/admin/categories'/>"><div class="stat-top"><span>Tổng danh mục</span><span class="stat-icon icon-blue" aria-hidden="true"><svg viewBox="0 0 24 24"><path d="M3 7a2 2 0 0 1 2-2h5l2 2h7a2 2 0 0 1 2 2v10H3Z"/><path d="M3 10h18"/></svg></span></div><strong class="stat-value"><c:out value="${categoryCount}"/></strong></a></div>
    <div class="col-md-4"><a class="stat-card" href="<c:url value='/admin/users'/>"><div class="stat-top"><span>Tổng người dùng</span><span class="stat-icon icon-purple" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M16 5a3 3 0 0 1 0 6M18 15a5 5 0 0 1 3 4v2"/></svg></span></div><strong class="stat-value"><c:out value="${userCount}"/></strong></a></div>
    <div class="col-md-4"><div class="stat-card"><div class="stat-top"><span>Tài khoản hoạt động</span><span class="stat-icon icon-green" aria-hidden="true"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m8 12 3 3 5-6"/></svg></span></div><strong class="stat-value"><c:out value="${activeUserCount}"/></strong></div></div>
</section>
<section class="mt-4"><div class="section-heading"><h2>Thao tác nhanh</h2></div><div class="row g-4"><div class="col-md-6"><a class="quick-card" href="<c:url value='/admin/categories/new'/>"><span class="quick-icon icon-blue" aria-hidden="true">＋</span><div><h3>Thêm danh mục mới</h3></div><span class="quick-arrow" aria-hidden="true">→</span></a></div><div class="col-md-6"><a class="quick-card" href="<c:url value='/admin/users/new'/>"><span class="quick-icon icon-purple" aria-hidden="true">＋</span><div><h3>Tạo tài khoản người dùng</h3></div><span class="quick-arrow" aria-hidden="true">→</span></a></div></div></section>
</body></html>
