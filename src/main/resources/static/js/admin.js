document.addEventListener('submit', function (event) {
    var form = event.target;
    if (form.matches('[data-confirm-delete]') && !window.confirm('Bạn có chắc muốn xóa bản ghi này? Thao tác này không thể hoàn tác.')) {
        event.preventDefault();
    }
});

document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-toggle-password]');
    if (!button) return;
    var input = document.getElementById(button.getAttribute('data-toggle-password'));
    if (!input) return;
    var show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    button.textContent = show ? 'Ẩn' : 'Hiện';
    button.setAttribute('aria-pressed', String(show));
    button.setAttribute('aria-label', show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu');
});
