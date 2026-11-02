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
                    if (response.data && response.data.token) {
                        sessionStorage.setItem('cc_auth_token', response.data.token);
                    }
                    if (response.data) {
                        sessionStorage.setItem('cc_user', JSON.stringify(response.data));
                    }
                    showAlert('Signed in successfully! Redirecting...', 'success');
                    setTimeout(() => {
                        window.location.href = '/chat';
                    }, 400);
                } else {
                    showAlert(response.message || 'Login failed', 'danger');
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Sign In';
                }
            } catch (err) {
                const message = (err.data && err.data.message) || err.message || 'Invalid credentials. Please verify and try again.';
                showAlert(message, 'danger');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Sign In';
            }
        });
    }

    // ----------------------------------------------------
    // Register Form Handler with Live Validation
    // ----------------------------------------------------
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        const phoneInput = document.getElementById('phoneNumber');
        const phoneFeedback = document.getElementById('phoneFeedback');
        const emailInput = document.getElementById('email');
        const emailFeedback = document.getElementById('emailFeedback');
        const passwordInput = document.getElementById('password');
        const confirmPasswordInput = document.getElementById('confirmPassword');
        const confirmPasswordFeedback = document.getElementById('confirmPasswordFeedback');
        const strengthMeterFill = document.getElementById('strengthMeterFill');
        const strengthStatusText = document.getElementById('strengthStatusText');

        const ruleLength = document.getElementById('ruleLength');
        const ruleUpper = document.getElementById('ruleUpper');
        const ruleLower = document.getElementById('ruleLower');
        const ruleDigit = document.getElementById('ruleDigit');
        const ruleSpecial = document.getElementById('ruleSpecial');

        // Toggle password visibility
        document.getElementById('togglePasswordBtn')?.addEventListener('click', () => {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
        });

        document.getElementById('toggleConfirmPasswordBtn')?.addEventListener('click', () => {
            const isPassword = confirmPasswordInput.type === 'password';
            confirmPasswordInput.type = isPassword ? 'text' : 'password';
        });

        // 1. Live Mobile Number Validation (Indian 10-digit series 6-9)
        phoneInput?.addEventListener('input', (e) => {
            // Only allow digits
            e.target.value = e.target.value.replace(/\D/g, '').slice(0, 10);
            const val = e.target.value;

            if (!val) {
                setValidationState(phoneInput, phoneFeedback, '', 'Enter your 10-digit mobile number starting with 6, 7, 8, or 9');
                return;
            }

            if (/^[6-9]\d{9}$/.test(val)) {
                setValidationState(phoneInput, phoneFeedback, 'valid', '✓ Valid 10-digit Indian mobile number');
            } else if (val.length === 10) {
                setValidationState(phoneInput, phoneFeedback, 'invalid', 'Mobile number must start with 6, 7, 8, or 9');
            } else {
                setValidationState(phoneInput, phoneFeedback, '', `${val.length}/10 digits entered`);
            }
        });

        // 2. Live Email Validation
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        emailInput?.addEventListener('input', (e) => {
            const val = e.target.value.trim();
            if (!val) {
                setValidationState(emailInput, emailFeedback, '', 'Enter a valid email address (e.g. name@domain.com)');
                return;
            }

            if (emailRegex.test(val)) {
                setValidationState(emailInput, emailFeedback, 'valid', '✓ Valid email address format');
            } else {
                setValidationState(emailInput, emailFeedback, 'invalid', 'Please enter a well-formed email (e.g. name@domain.com)');
            }
        });

        // 3. Live Password Strength Calculation & Checklist
        function evaluatePassword(pwd) {
            const checks = {
                length: pwd.length >= 8,
                upper: /[A-Z]/.test(pwd),
                lower: /[a-z]/.test(pwd),
                digit: /[0-9]/.test(pwd),
                special: /[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?~`]/.test(pwd)
            };

            updateRule(ruleLength, checks.length);
            updateRule(ruleUpper, checks.upper);
            updateRule(ruleLower, checks.lower);
            updateRule(ruleDigit, checks.digit);
            updateRule(ruleSpecial, checks.special);

            const passedCount = Object.values(checks).filter(Boolean).length;

            if (!pwd) {
                strengthMeterFill.style.width = '0%';
                strengthMeterFill.className = 'strength-meter-fill';
                strengthStatusText.textContent = 'None';
                strengthStatusText.className = 'strength-status-text';
                passwordInput.classList.remove('is-valid', 'is-invalid');
                return 0;
            }

            if (passedCount <= 2) {
                strengthMeterFill.style.width = '30%';
                strengthMeterFill.className = 'strength-meter-fill weak';
                strengthStatusText.textContent = 'Weak';
                strengthStatusText.className = 'strength-status-text weak';
                passwordInput.classList.add('is-invalid');
                passwordInput.classList.remove('is-valid');
            } else if (passedCount <= 4) {
                strengthMeterFill.style.width = '65%';
                strengthMeterFill.className = 'strength-meter-fill medium';
                strengthStatusText.textContent = 'Moderate';
                strengthStatusText.className = 'strength-status-text medium';
                passwordInput.classList.add('is-invalid');
                passwordInput.classList.remove('is-valid');
            } else {
                strengthMeterFill.style.width = '100%';
                strengthMeterFill.className = 'strength-meter-fill strong';
                strengthStatusText.textContent = 'Strong ✓';
                strengthStatusText.className = 'strength-status-text strong';
                passwordInput.classList.add('is-valid');
                passwordInput.classList.remove('is-invalid');
            }

            return passedCount;
        }

        function updateRule(el, passed) {
            if (!el) return;
            if (passed) {
                el.className = 'strength-item passed';
                el.querySelector('.rule-icon').textContent = '✓';
            } else {
                el.className = 'strength-item unmet';
                el.querySelector('.rule-icon').textContent = '○';
            }
        }

        passwordInput?.addEventListener('input', () => {
            evaluatePassword(passwordInput.value);
            checkPasswordMatch();
        });

        // 4. Live Confirm Password Matching
        function checkPasswordMatch() {
            const pwd = passwordInput.value;
            const confirm = confirmPasswordInput.value;

            if (!confirm) {
                setValidationState(confirmPasswordInput, confirmPasswordFeedback, '', 'Passwords must match');
                return false;
            }

            if (pwd === confirm) {
                setValidationState(confirmPasswordInput, confirmPasswordFeedback, 'valid', '✓ Passwords match');
                return true;
            } else {
                setValidationState(confirmPasswordInput, confirmPasswordFeedback, 'invalid', 'Passwords do not match');
                return false;
            }
        }

        confirmPasswordInput?.addEventListener('input', checkPasswordMatch);

        function setValidationState(inputEl, feedbackEl, state, message) {
            if (inputEl) {
                inputEl.classList.remove('is-valid', 'is-invalid');
                if (state === 'valid') inputEl.classList.add('is-valid');
                if (state === 'invalid') inputEl.classList.add('is-invalid');
            }
            if (feedbackEl) {
                feedbackEl.className = `validation-hint ${state}`;
                feedbackEl.textContent = message;
            }
        }

        // 5. Submit Event Handler
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearAlerts();

            const displayName = document.getElementById('displayName').value.trim();
            const rawPhone = phoneInput.value.trim();
            const email = emailInput.value.trim();
            const password = passwordInput.value;
            const confirmPassword = confirmPasswordInput.value;
            const submitBtn = document.getElementById('registerSubmitBtn') || registerForm.querySelector('button[type="submit"]');

            if (!displayName || !rawPhone || !email || !password || !confirmPassword) {
                showAlert('All fields are required', 'danger');
                return;
            }

            // Mobile validation
            if (!/^[6-9]\d{9}$/.test(rawPhone)) {
                showAlert('Please enter a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9', 'danger');
                phoneInput.focus();
                return;
            }

            // Email validation
            if (!emailRegex.test(email)) {
                showAlert('Please enter a valid email address (e.g. name@domain.com)', 'danger');
                emailInput.focus();
                return;
            }

            // Strong password check
            const strengthScore = evaluatePassword(password);
            if (strengthScore < 5) {
                showAlert('Please choose a strong password fulfilling all 5 security requirements', 'danger');
                passwordInput.focus();
                return;
            }

            // Passwords match check
            if (password !== confirmPassword) {
                showAlert('Passwords do not match', 'danger');
                confirmPasswordInput.focus();
                return;
            }

            const formattedPhone = '+91 ' + rawPhone;

            try {
                submitBtn.disabled = true;
                submitBtn.textContent = 'Creating account...';

                const regResponse = await Api.auth.register({ 
                    displayName, 
                    phoneNumber: formattedPhone, 
                    email, 
                    password 
                });

                if (regResponse && regResponse.success) {
                    // Automatically log the user in using email or mobile
                    const loginRes = await Api.auth.login({ usernameOrEmail: email, password });
                    if (loginRes && loginRes.data && loginRes.data.token) {
                        sessionStorage.setItem('cc_auth_token', loginRes.data.token);
                    }
                    if (loginRes && loginRes.data) {
                        sessionStorage.setItem('cc_user', JSON.stringify(loginRes.data));
                    }
                    showAlert('Account created successfully! Redirecting to chat...', 'success');
                    setTimeout(() => {
                        window.location.href = '/chat';
                    }, 500);
                } else {
                    showAlert(regResponse.message || 'Registration failed', 'danger');
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'Create Account';
                }
            } catch (err) {
                const message = (err.data && err.data.message) || err.message || 'Registration failed';
                showAlert(message, 'danger');
                submitBtn.disabled = false;
                submitBtn.textContent = 'Create Account';
            }
        });
    }

    function showAlert(message, type = 'danger') {
        const alertBox = document.getElementById('alertBox');
        if (!alertBox) return;

        alertBox.className = `alert-box alert-${type}`;
        alertBox.textContent = message;
        alertBox.classList.remove('hidden');
    }

    function clearAlerts() {
        const alertBox = document.getElementById('alertBox');
        if (!alertBox) return;
        alertBox.classList.add('hidden');
        alertBox.textContent = '';
    }
});
