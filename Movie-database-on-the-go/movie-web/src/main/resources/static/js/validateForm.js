function validateForm() {
    var name = document.getElementById("name").value;
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    var email = document.getElementById("email").value;
    var isValid = true;

    // Név ellenőrzése
    if (name.length < 3 || name.length > 32 || !/^[a-zA-Z][a-zA-Z0-9.,_-]{2,31}$/.test(name)) {
        document.getElementById("nameError").innerText = "A név legalább 3 és legfeljebb 32 karakter hosszú lehet, és csak betűket, számokat, valamint ., _ vagy - karaktereket tartalmazhat, és betűvel kell kezdődnie.";
        isValid = false;
    } else {
        document.getElementById("nameError").innerText = "";
    }

    // Felhasználónév ellenőrzése
    if (username.length < 3 || username.length > 32 || !/^[a-zA-Z][a-zA-Z0-9.,_-]{2,31}$/.test(username)) {
        document.getElementById("usernameError").innerText = "A felhasználónév legalább 3 és legfeljebb 32 karakter hosszú lehet, és csak betűket, számokat, valamint ., _ vagy - karaktereket tartalmazhat, és betűvel kell kezdődnie.";
        isValid = false;
    } else {
        document.getElementById("usernameError").innerText = "";
    }

    // Jelszó ellenőrzése
    if (password.length < 6 || !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{6,}$/.test(password)) {
        document.getElementById("passwordError").innerText = "A jelszónak legalább 6 karakter hosszúnak kell lennie, és tartalmaznia kell legalább egy kisbetűt, egy nagybetűt és egy számot.";
        isValid = false;
    } else {
        document.getElementById("passwordError").innerText = "";
    }

    // E-mail ellenőrzése
    if (!email || !/^\S+@\S+\.\S+$/.test(email)) {
        document.getElementById("emailError").innerText = "Kérlek adj meg egy valós e-mail címet.";
        isValid = false;
    } else {
        document.getElementById("emailError").innerText = "";
    }

    return isValid;
}
