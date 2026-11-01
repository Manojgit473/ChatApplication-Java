/**
 * Connect Chat - Authentication Page Controller (Login & Registration)
 */

document.addEventListener('DOMContentLoaded', () => {
    // ----------------------------------------------------
    // Login Form Handler
    // ----------------------------------------------------
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAlerts();

            const usernameOrEmail = document.getElementById('usernameOrEmail').value.trim();
            const password = document.getElementById('password').value;
            const submitBtn = loginForm.querySelector('button[type="submit"]');

            if (!usernameOrEmail || !password) {
                showAlert('Please fill in both fields', 'danger');
                return;
            }

            try {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Signing in...';

                const response = await Api.auth.login({ usernameOrEmail, password });
                if (response && response.success) {
                    showAlert('Signed in successfully! Redirecting...', 'success');
                    setTimeout(() => {
                        window.location.href = '/chat';
                    }, 500);
                } else {
                    showAlert(response.message || 'Login failed', 'danger');
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Sign In';
                }
            } catch (err) {
                const message = (err.data && err.data.message) || err.message || 'Invalid username or password';
                showAlert(message, 'danger');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Sign In';
            }
        });
    }

    // ----------------------------------------------------
    // Register Form Handler
    // ----------------------------------------------------
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAlerts();

            const displayName = document.getElementById('displayName').value.trim();
            const username = document.getElementById('username').value.trim();
            const email = document.getElementById('email').value.trim();
            const password = document.getElementById('password').value;
            const confirmPassword = document.getElementById('confirmPassword').value;
            const submitBtn = registerForm.querySelector('button[type="submit"]');

            if (!displayName || !username || !email || !password || !confirmPassword) {
                showAlert('All fields are required', 'danger');
                return;
            }

            if (password !== confirmPassword) {
                showAlert('Passwords do not match', 'danger');
                return;
            }

            if (password.length < 6) {
                showAlert('Password must be at least 6 characters long', 'danger');
                return;
            }

            try {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Creating account...';

                const regResponse = await Api.auth.register({ displayName, username, email, password });
                if (regResponse && regResponse.success) {
                    // Automatically log the user in
                    await Api.auth.login({ usernameOrEmail: username, password });
                    showAlert('Account created! Redirecting to workspace...', 'success');
                    setTimeout(() => {
                        window.location.href = '/chat';
                    }, 600);
                } else {
                    showAlert(regResponse.message || 'Registration failed', 'danger');
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Create Account';
                }
            } catch (err) {
                let msg = (err.data && err.data.message) || err.message || 'Registration failed';
                if (err.data && err.data.validationErrors) {
                    msg = Object.values(err.data.validationErrors).join('. ');
                }
                showAlert(msg, 'danger');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Account';
            }
        });
    }

    function showAlert(message, type) {
        const alertBox = document.getElementById('alertBox');
        if (!alertBox) return;

        alertBox.className = `alert-box alert-${type}`;
        alertBox.textContent = message;
        alertBox.classList.remove('hidden');
    }

    function clearAlerts() {
        const alertBox = document.getElementById('alertBox');
        if (alertBox) {
            alertBox.classList.add('hidden');
            alertBox.textContent = '';
        }
    }
});

