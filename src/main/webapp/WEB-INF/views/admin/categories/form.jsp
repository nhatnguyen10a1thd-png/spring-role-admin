<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html><html lang="vi"><head><title>${editMode ? 'Chỉnh sửa danh mục' : 'Thêm danh mục'}</title></head><body>
<a class="back-link" href="<c:url value='/admin/categories'/>"><span aria-hidden="true">←</span> Trở về danh sách danh mục</a>
<div class="page-heading"><div><h1>${editMode ? 'Chỉnh sửa danh mục' : 'Thêm danh mục'}</h1></div></div>
<%@ include file="../../fragments/flash.jsp" %>
<c:choose><c:when test="${editMode}"><c:url var="formAction" value="/admin/categories/${categoryId}"/></c:when><c:otherwise><c:url var="formAction" value="/admin/categories"/></c:otherwise></c:choose>
<div class="form-layout"><section class="panel form-panel"><div class="panel-header"><div><h2>Thông tin danh mục</h2></div></div>
<form:form modelAttribute="categoryForm" method="post" action="${formAction}" htmlEscape="true">
    <div class="form-body"><form:errors path="*" element="div" cssClass="alert alert-danger" htmlEscape="true"/>
        <div class="mb-4"><label for="name" class="form-label">Tên danh mục <span class="required">*</span></label><form:input path="name" id="name" cssClass="form-control" cssErrorClass="form-control is-invalid" maxlength="120" required="required" placeholder="Ví dụ: Công nghệ" aria-describedby="name-help name-error"/><form:errors path="name" id="name-error" cssClass="field-error" htmlEscape="true"/><div id="name-help" class="form-text">Tối đa 120 ký tự.</div></div>
        <div class="mb-4"><label for="description" class="form-label">Mô tả</label><form:textarea path="description" id="description" cssClass="form-control" cssErrorClass="form-control is-invalid" rows="5" maxlength="1000" placeholder="Nhập mô tả danh mục..." aria-describedby="description-help description-error"/><form:errors path="description" id="description-error" cssClass="field-error" htmlEscape="true"/><div id="description-help" class="form-text">Tối đa 1.000 ký tự.</div></div>
        <div><label class="form-label" for="active">Trạng thái</label><div class="form-check form-switch"><form:checkbox path="active" id="active" cssClass="form-check-input" role="switch"/><label class="form-check-label" for="active">Đang hoạt động</label></div><form:errors path="active" cssClass="field-error" htmlEscape="true"/></div>
    </div>
    <div class="form-actions"><div class="d-flex gap-2 ms-auto"><a class="btn btn-outline-secondary" href="<c:url value='/admin/categories'/>">Hủy</a><button type="submit" class="btn btn-primary">${editMode ? 'Lưu thay đổi' : 'Tạo danh mục'}</button></div></div>
</form:form></section></div>
</body></html>
