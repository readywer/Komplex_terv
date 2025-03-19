function uploadFilm() {
    let fileInput = document.getElementById("file");
    let imageInput = document.getElementById("imageFile");

    if (fileInput.files.length === 0) {
        alert("Please select a video file to upload.");
        return;
    }

    let file = fileInput.files[0];
    let imageFile = imageInput.files.length > 0 ? imageInput.files[0] : null;

    let formData = new FormData();
    formData.append("file", file);
    if (imageFile) {
        formData.append("imageFile", imageFile);
    }

    formData.append("name", document.getElementById("name").value);
    formData.append("description", document.getElementById("description").value);
    let actors = document.getElementById("actors").value.split(",").map(actor => actor.trim());
    actors.forEach(actor => formData.append("actors", actor));

    formData.append("releaseDate", document.getElementById("releaseDate").value);
    formData.append("recommendedAge", document.getElementById("recommendedAge").value);
    formData.append("quality", document.getElementById("quality").value);

    let categorySelect = document.getElementById("categories");
    let selectedCategories = Array.from(categorySelect.selectedOptions).map(opt => opt.value);
    formData.append("categories", selectedCategories);

    let xhr = new XMLHttpRequest();
    xhr.open("POST", "/film_add", true);

    let progressContainer = document.getElementById("progressContainer");
    let progressBar = document.getElementById("progressBar");
    let progressInfo = document.getElementById("progressInfo");
    let uploadedSizeText = document.getElementById("uploadedSize");
    let totalSizeText = document.getElementById("totalSize");
    let uploadSpeedText = document.getElementById("uploadSpeed");

    progressContainer.style.display = "block";
    progressInfo.style.display = "block";
    totalSizeText.textContent = ((file.size + (imageFile ? imageFile.size : 0)) / 1024 / 1024).toFixed(2) + " MB";

    let startTime = Date.now();

    xhr.upload.addEventListener("progress", function (event) {
        if (event.lengthComputable) {
            let percent = Math.round((event.loaded / event.total) * 100);
            let uploadedMB = (event.loaded / 1024 / 1024).toFixed(2);
            let totalMB = (event.total / 1024 / 1024).toFixed(2);
            let elapsedTime = (Date.now() - startTime) / 1000;
            let speed = (uploadedMB / elapsedTime).toFixed(2); // MB/s

            progressBar.style.width = percent + "%";
            progressBar.innerText = percent + "%";
            uploadedSizeText.textContent = uploadedMB + " MB";
            uploadSpeedText.textContent = speed + " MB/s";
        }
    });

    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                alert("Upload successful!");
                location.href = "/films";
            } else {
                alert("Upload failed: " + xhr.responseText);
            }
        }
    };

    xhr.send(formData);
}
