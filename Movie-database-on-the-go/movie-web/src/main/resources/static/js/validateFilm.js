function validateFilm() {
    var name = document.getElementById("name").value;
    var recommendedAge = parseInt(document.getElementById("recommendedAge").value);
    var file = document.getElementById("file").value;
    var imageFile = document.getElementById("imageFile").value;
    var isValid = true;

    // Név ellenőrzése
    if (!/^[a-zA-Z][a-zA-Z0-9.,_-]{1,32}$/.test(name)) {
        document.getElementById("nameError").innerText = "A név legalább 1 és legfeljebb 32 karakter hosszú lehet, és csak betűket, számokat, valamint ., _ vagy - karaktereket tartalmazhat, és betűvel kell kezdődnie.";
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

    // Fájl ellenőrzése
    if (!file || file.trim() === "" || !isValidExtension(file, [".mp4", ".webm", ".ogg"])) {
        document.getElementById("fileError").innerText = "Invalid file format!";
        isValid = false;
    } else {
        document.getElementById("fileError").innerText = "";
    }

    if (imageFile.trim() !== "" && !isValidImageExtension(imageFile)) {
           document.getElementById("imageFileError").innerText = "Invalid image format!";
           isValid = false;
       } else {
           document.getElementById("imageFileError").innerText = "";
       }

    return isValid;
}

function isValidExtension(filename, extensions) {
    var extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
    return extensions.includes(extension);
}

function isValidImageExtension(filename) {
    var extensions = [".jpg", ".jpeg", ".png", ".gif",""];
    return isValidExtension(filename, extensions);
}