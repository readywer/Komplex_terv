function validateFilmForm() {
    var name = document.getElementById("name").value;
    var description = document.getElementById("description").value;
    var categories = document.getElementById("categories").selectedOptions;
    var actors = document.getElementById("actors").value;
    var recommendedAge = parseInt(document.getElementById("recommendedAge").value);
    var file = document.getElementById("file").value;
    var imageFile = document.getElementById("imageFile").value;
    var isValid = true;

    // Név ellenőrzése
    if (!isValidName(name)) {
        document.getElementById("nameError").innerText = "Invalid name!";
        isValid = false;
    } else {
        document.getElementById("nameError").innerText = "";
    }

    // Leírás ellenőrzése
    if (!description || description.trim() === "") {
        document.getElementById("descriptionError").innerText = "Invalid description!";
        isValid = false;
    } else {
        document.getElementById("descriptionError").innerText = "";
    }

    // Kategóriák ellenőrzése
    if (categories.length === 0) {
        document.getElementById("categoriesError").innerText = "Invalid categories!";
        isValid = false;
    } else {
        document.getElementById("categoriesError").innerText = "";
    }

    // Színészek ellenőrzése
    if (!actors || actors.trim() === "") {
        document.getElementById("actorsError").innerText = "Invalid actors!";
        isValid = false;
    } else {
        document.getElementById("actorsError").innerText = "";
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

    // Képfájl ellenőrzése
    if (!imageFile || imageFile.trim() === "" || !isValidImageExtension(imageFile)) {
        document.getElementById("imageFileError").innerText = "Invalid image format!";
        isValid = false;
    } else {
        document.getElementById("imageFileError").innerText = "";
    }

    return isValid;
}

function isValidName(name) {
    return name && name.trim() !== "" && !name.includes("/") && !name.includes("\\");
}

function isValidExtension(filename, extensions) {
    var extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
    return extensions.includes(extension);
}

function isValidImageExtension(filename) {
    var extensions = [".jpg", ".jpeg", ".png", ".gif",""];
    return isValidExtension(filename, extensions);
}