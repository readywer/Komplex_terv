document.addEventListener("DOMContentLoaded", function () {
    const darkModeSwitch = document.getElementById("darkModeSwitch");
    const html = document.documentElement;
    const body = document.body;

    function updateDarkModeClasses(enable) {
        const dynamicElements = document.querySelectorAll(".dynamic-bg");
        dynamicElements.forEach(element => {
            if (enable) {
                element.classList.add("bg-dark", "text-light");
                element.classList.remove("bg-light", "text-dark");
            } else {
                element.classList.add("bg-light", "text-dark");
                element.classList.remove("bg-dark", "text-light");
            }
        });
    }

    function enableDarkMode() {
        html.setAttribute("data-bs-theme", "dark");
        body.classList.add("bg-dark", "text-light");
        updateDarkModeClasses(true);
        localStorage.setItem("darkMode", "enabled");
    }

    function disableDarkMode() {
        html.setAttribute("data-bs-theme", "light");
        body.classList.remove("bg-dark", "text-light");
        updateDarkModeClasses(false);
        localStorage.setItem("darkMode", "disabled");
    }

    // Ellenőrizzük, hogy korábban milyen mód volt beállítva
    if (localStorage.getItem("darkMode") === "enabled") {
        enableDarkMode();
        darkModeSwitch.checked = true;
    } else {
        disableDarkMode();
    }

    // Ha a felhasználó módot vált
    darkModeSwitch.addEventListener("change", function () {
        if (this.checked) {
            enableDarkMode();
        } else {
            disableDarkMode();
        }
    });
});
