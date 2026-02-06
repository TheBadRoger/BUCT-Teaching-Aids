document.addEventListener('DOMContentLoaded', function () {
    const loginForm = document.getElementById('loginForm');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const errorMsg = document.getElementById('errorMsg');

    function showError(msg) {
        if (!errorMsg) return;
        errorMsg.textContent = msg;
        errorMsg.style.display = 'block';
    }

    function clearError() {
        if (!errorMsg) return;
        errorMsg.textContent = '';
        errorMsg.style.display = 'none';
    }

    function validateLogin() {
        const u = usernameInput.value.trim();
        const p = passwordInput.value;

        if (!u) {
            showError('请输入学号/工号');
            usernameInput.classList.add('input-error');
            usernameInput.focus();
            return false;
        }
        if (u.length < 3) {
            showError('账号长度应至少 3 位');
            usernameInput.classList.add('input-error');
            usernameInput.focus();
            return false;
        }
        if (!p) {
            showError('请输入密码');
            passwordInput.classList.add('input-error');
            passwordInput.focus();
            return false;
        }
        if (p.length < 6) {
            showError('密码长度至少 6 位');
            passwordInput.classList.add('input-error');
            passwordInput.focus();
            return false;
        }
        return true;
    }

    if (usernameInput) {
        usernameInput.addEventListener('input', function () {
            usernameInput.classList.remove('input-error');
            clearError();
        });
    }
    if (passwordInput) {
        passwordInput.addEventListener('input', function () {
            passwordInput.classList.remove('input-error');
            clearError();
        });
    }

    if (loginForm) {
        loginForm.addEventListener('submit', function (e) {
            e.preventDefault();
            clearError();
            if (!validateLogin()) return;
            // use AJAX to call backend login API, then redirect on success
            const username = usernameInput.value.trim();
            const password = passwordInput.value;

            const params = new URLSearchParams();
            params.append('username', username);
            params.append('password', password);

            fetch('/api/aijudegment/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                },
                body: params.toString()
            })
                .then(response => response.json())
                .then(data => {
                    if (data && data.code === 0) {
                        window.location.href = 'AIJudge.html';
                    } else {
                        showError((data && data.msg) || '登录失败：用户名或密码错误');
                    }
                })
                .catch(err => {
                    console.error('login error', err);
                    showError('网络错误，请稍后重试');
                });
        });
    }
});
