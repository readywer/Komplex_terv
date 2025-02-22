 var originalFilmsOrder = []; // Eredeti filmek sorrendje

    function saveOriginalOrder() {
        // Elmentjük az eredeti filmek sorrendjét
        var filmsContainer = document.getElementById("filmTiles");
        var films = filmsContainer.getElementsByClassName("col-sm-3");
        for (var i = 0; i < films.length; i++) {
            originalFilmsOrder.push(films[i]);
        }
    }

    function sortFilms() {
        var sortCriteria = document.getElementById("sortSelect").value;
        var filmsContainer = document.getElementById("filmTiles");
        var films = filmsContainer.getElementsByClassName("col-sm-3");
        var filmsArray = Array.from(films);

        if (sortCriteria === 'default') {
            // Alapértelmezett rendezés: az eredeti sorrend felhasználása
            filmsArray.sort(function(a, b) {
                var indexA = originalFilmsOrder.indexOf(a);
                var indexB = originalFilmsOrder.indexOf(b);
                return indexA - indexB;
            });
        } else if (sortCriteria === 'name') {
            // Rendezés a név szerint
            filmsArray.sort(function(a, b) {
                var nameA = a.querySelector(".card-title").textContent.toUpperCase();
                var nameB = b.querySelector(".card-title").textContent.toUpperCase();
                if (nameA < nameB) {
                    return -1;
                }
                if (nameA > nameB) {
                    return 1;
                }
                return 0;
            });
        } else if (sortCriteria === 'releaseDate') {
            // Rendezés a megjelenési dátum szerint
            filmsArray.sort(function(a, b) {
                var dateA = new Date(a.getAttribute("data-release-date"));
                var dateB = new Date(b.getAttribute("data-release-date"));
                return dateA - dateB;
            });
        }

        // Töröljük a meglévő filmeket
        while (filmsContainer.firstChild) {
            filmsContainer.removeChild(filmsContainer.firstChild);
        }

        // Hozzáadjuk a rendezett filmeket újra
        for (var i = 0; i < filmsArray.length; i++) {
            filmsContainer.appendChild(filmsArray[i]);
        }
    }

    function filterFilms() {
    var input = document.getElementById("searchInput").value.toLowerCase();
    var films = document.querySelectorAll("#filmTiles .col-sm-3");
    var found = false;

    films.forEach(function(film) {
        var name = film.getAttribute("data-name").toLowerCase();
        var description = film.getAttribute("data-description").toLowerCase();
        var categories = film.getAttribute("data-categories").toLowerCase();
        var actors = film.getAttribute("data-actors").toLowerCase();
        var recommendedAge = film.getAttribute("data-recommended-age").toLowerCase();
        var releaseDate = film.getAttribute("data-release-date").toLowerCase();

        // Ellenőrizzük, hogy a keresett szó benne van-e valamelyik mezőben
        if (name.includes(input) || description.includes(input) || categories.includes(input) ||
            actors.includes(input) || recommendedAge.includes(input) || releaseDate.includes(input)) {
            film.style.display = "block";
            found = true;
        } else {
            film.style.display = "none";
        }
    });

    // Ha nincs találat, jelenjen meg az üzenet
    var noResultsMessage = document.getElementById("noResultsMessage");
    if (found) {
        noResultsMessage.style.display = "none";
        } else {
        noResultsMessage.style.display = "block";
        }
    }


    // Első híváskor elmentjük az eredeti sorrendet
    window.onload = saveOriginalOrder;