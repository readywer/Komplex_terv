var originalClientsOrder = []; // Eredeti kliensek sorrendje

function saveOriginalOrder() {
    // Elmentjük az eredeti kliensek sorrendjét
    var clientsContainer = document.getElementById("clientTiles");
    var clients = clientsContainer.getElementsByClassName("col-sm-3");
    for (var i = 0; i < clients.length; i++) {
        originalClientsOrder.push(clients[i]);
    }
}

function sortClients() {
    var sortCriteria = document.getElementById("sortSelect").value;
    var clientsContainer = document.getElementById("clientTiles");
    var clients = clientsContainer.getElementsByClassName("col-sm-3");
    var clientsArray = Array.from(clients);

    if (sortCriteria === 'default') {
        // Alapértelmezett sorrend visszaállítása
        clientsArray.sort(function(a, b) {
            var indexA = originalClientsOrder.indexOf(a);
            var indexB = originalClientsOrder.indexOf(b);
            return indexA - indexB;
        });
    } else if (sortCriteria === 'name') {
        // Rendezés név szerint
        clientsArray.sort(function(a, b) {
            var nameA = a.getAttribute("data-name").toUpperCase();
            var nameB = b.getAttribute("data-name").toUpperCase();
            return nameA.localeCompare(nameB);
        });
    }

    // Eredeti elemek törlése és újra hozzáadása
    while (clientsContainer.firstChild) {
        clientsContainer.removeChild(clientsContainer.firstChild);
    }

    clientsArray.forEach(client => clientsContainer.appendChild(client));
}

function filterClients() {
    var input = document.getElementById("searchInput").value.toLowerCase();
    var clients = document.querySelectorAll("#clientTiles .col-sm-3");
    var found = false;

    clients.forEach(function(client) {
        var username = client.getAttribute("data-username").toLowerCase();
        var name = client.getAttribute("data-name").toLowerCase();
        var email = client.getAttribute("data-email").toLowerCase();

        // Ellenőrizzük, hogy a keresett szó szerepel-e valamelyik mezőben
        if (username.includes(input) || name.includes(input) || email.includes(input)) {
            client.style.display = "block";
            found = true;
        } else {
            client.style.display = "none";
        }
    });

    // Ha nincs találat, megjelenítjük az üzenetet
    var noResultsMessage = document.getElementById("noResultsMessage");
    noResultsMessage.style.display = found ? "none" : "block";
}

// Első betöltéskor elmentjük az eredeti sorrendet
window.onload = saveOriginalOrder;
