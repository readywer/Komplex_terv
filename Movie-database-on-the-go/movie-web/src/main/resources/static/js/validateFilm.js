function validateFilm() {
    var name = document.getElementById("name").value.trim();
    var recommendedAge = parseInt(document.getElementById("recommendedAge").value);
    var file = document.getElementById("file").value.trim();
    var imageFile = document.getElementById("imageFile").value.trim();
    var isValid = true;

    // Név ellenőrzése
    if (name === "") {
        document.getElementById("nameError").innerText = "The name cannot be empty or contain only spaces!";
        isValid = false;
    } else {
        document.getElementById("nameError").innerText = "";
    }

    // Ajánlott életkor ellenőrzése
    if (isNaN(recommendedAge) || recommendedAge < 0 || recommendedAge > 18) {
        document.getElementById("recommendedAgeError").innerText = "Invalid recommended age!";
        isValid = false;
    } else {
        document.getElementById("recommendedAgeError").innerText = "";
    }

    // Video fájl ellenőrzése
    if (file === "" || !isValidVideoExtension(file)) {
        document.getElementById("fileError").innerText = "You can only upload videos!";
        isValid = false;
    } else {
        document.getElementById("fileError").innerText = "";
    }

    // Kép ellenőrzése
    if (imageFile !== "" && !isValidImageExtension(imageFile)) {
        document.getElementById("imageFileError").innerText = "You can only upload pictures!";
        isValid = false;
    } else {
        document.getElementById("imageFileError").innerText = "";
    }

    return isValid;
}

// Csak videófájlokat engedélyezünk
function isValidVideoExtension(filename) {
    var videoExtensions = [".mp4", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".webm", ".ogv"];
    return isValidExtension(filename, videoExtensions);
}

// Csak képfájlokat engedélyezünk
function isValidImageExtension(filename) {
    var imageExtensions = [".jpg", ".jpeg", ".png", ".gif"];
    return isValidExtension(filename, imageExtensions);
}

// Általános fájl-kiterjesztés ellenőrző
function isValidExtension(filename, extensions) {
    var extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
    return extensions.includes(extension);
}
