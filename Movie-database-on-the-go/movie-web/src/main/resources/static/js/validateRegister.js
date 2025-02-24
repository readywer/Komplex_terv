function validateRegister() {
    var name = document.getElementById("name").value;
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    var email = document.getElementById("email").value;
    var isValid = true;

    // Name validation
    if (name.length < 3 || name.length > 32 || !/^[a-zA-Z][a-zA-Z0-9.,_-]{2,31}$/.test(name)) {
        document.getElementById("nameError").innerText =
            "The name must be between 3 and 32 characters long and can only contain letters, numbers, and ., _ or - characters. It must start with a letter.";
        isValid = false;
    } else {
        document.getElementById("nameError").innerText = "";
    }

    // Username validation
    if (username.length < 3 || username.length > 32 || !/^[a-zA-Z][a-zA-Z0-9.,_-]{2,31}$/.test(username)) {
        document.getElementById("usernameError").innerText =
            "The username must be between 3 and 32 characters long and can only contain letters, numbers, and ., _ or - characters. It must start with a letter.";
        isValid = false;
    } else {
        document.getElementById("usernameError").innerText = "";
    }

    // Password validation
    if (password.length < 6 || !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/.test(password)) {
        document.getElementById("passwordError").innerText =
            "The password must be at least 6 characters long and contain at least one lowercase letter, one uppercase letter, and one number.";
        isValid = false;
    } else {
        document.getElementById("passwordError").innerText = "";
    }

    // Email validation
    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
        document.getElementById("emailError").innerText = "Please enter a valid email address.";
        isValid = false;
    } else {
        document.getElementById("emailError").innerText = "";
    }

    return isValid;
}
