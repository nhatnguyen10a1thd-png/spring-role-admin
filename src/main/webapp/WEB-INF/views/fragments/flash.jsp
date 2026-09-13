<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:if test="${not empty successMessage}"><div class="alert alert-success alert-dismissible fade show" role="status"><c:out value="${successMessage}"/><button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng thông báo"></button></div></c:if>
<c:if test="${not empty errorMessage}"><div class="alert alert-danger alert-dismissible fade show" role="alert"><c:out value="${errorMessage}"/><button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng thông báo"></button></div></c:if>
