const fields = ["password", "confirm-password"]
const eyes = ["password-eye", "confirm-password-eye"]

for (let i = 0; i < fields.length; i++) {
    const passwordField = document.getElementById(fields[i]);
    const togglePassword = document.getElementById(eyes[i]);

    if (!passwordField || !togglePassword) continue;

    function reveal() {
        passwordField.type = "text";
        togglePassword.classList.remove("fa-eye");
        togglePassword.classList.add("fa-eye-slash");
    }

    function conceal() {
        passwordField.type = "password";
        togglePassword.classList.remove("fa-eye-slash");
        togglePassword.classList.add("fa-eye");
    }

    togglePassword.addEventListener("mousedown", function (e) {
        e.preventDefault(); // prevent input losing focus
        reveal();
    });

    togglePassword.addEventListener("mouseup", conceal);
    togglePassword.addEventListener("mouseleave", conceal);

    // touch support
    togglePassword.addEventListener("touchstart", function (e) {
        e.preventDefault();
        reveal();
    }, { passive: false });

    togglePassword.addEventListener("touchend", conceal);
}
