package org.movie.database.service;

import org.movie.database.domain.Client;
import org.movie.database.persistence.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClientService {
    @Autowired
    private FilmService filmService;
    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public boolean createClient(Client client) {
        if (isValidClient(client)) {
            createDirectory(client);
            clientRepository.save(client);
            return true;
        }
        return false;
    }

    private void createDirectory(Client client) {
        String folderPath = filmService.getStorageDir() + "/" + client.getUsername();
        try {
            Path directoryPath = Paths.get(folderPath);
            Files.createDirectories(directoryPath);
            String filePath = folderPath + "/film.json";
            Path file = Paths.get(filePath);
            if (!Files.exists(file)) {
                Files.createFile(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO hiba visszajelzés
    public boolean isValidClient(Client client) {
        if (!isValidName(client.getName())) {
            return false;
        }
        if (!isValidUsername(client.getUsername())) {
            return false;
        }
        if (!isValidPassword(client.getPassword())) {
            return false;
        }
        if (!isValidEmail(client.getEmail())) {
            return false;
        }
        return true;
    }

    private boolean isValidUsername(String username) {
        // Ellenőrizze, hogy a felhasználónév üres-e
        if (username.isEmpty()) {
            return false;
        }

        // Ellenőrizze a hosszt
        if (username.length() < 3 || username.length() > 32) {
            return false;
        }

        // Ellenőrizze, hogy a felhasználónév betűvel kezdődik
        if (!Character.isLetter(username.charAt(0))) {
            return false;
        }

        //Ha már létezik ilyen fehasznló mégegyszer ne lehessen létrehozni
        if (findClientByUsername(username) != null) {
            return false;
        }

        // Ellenőrizze az engedélyezett karaktereket
        Pattern pattern = Pattern.compile("^[a-zA-Z0-9.,_-]+$");
        Matcher matcher = pattern.matcher(username);
        return matcher.matches();
    }

    private boolean isValidName(String name) {
        // Ellenőrizze, hogy a felhasználónév üres-e
        if (name.isEmpty()) {
            return false;
        }

        // Ellenőrizze a hosszt
        if (name.length() < 3 || name.length() > 32) {
            return false;
        }
        // Ellenőrizze, hogy a felhasználónév betűvel kezdődik
        return Character.isLetter(name.charAt(0));
    }

    private boolean isValidPassword(String password) {
        // Ellenőrizze, hogy a jelszó üres-e
        if (password.isEmpty()) {
            return false;
        }

        // Ellenőrizze a hosszt
        if (password.length() < 6) {
            return false;
        }

        // Ellenőrizze legalább egy kisbetűt, egy nagybetűt és egy számot
        Pattern pattern = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$");
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();
    }

    private boolean isValidEmail(String email) {
        // Ellenőrizze, hogy az e-mail cím üres-e
        if (email.isEmpty()) {
            return false;
        }

        // Ellenőrizze az e-mail cím reguláris kifejezéssel
        Pattern pattern = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    public Client findClientByUsername(String username) {
        for (Client client : clientRepository.findAll()) {
            if (client.getUsername().equals(username)) {
                return client;
            }
        }
        return null;
    }

    public Client findProtectedClientByUsername(String username) {
        for (Client client : clientRepository.findAll()) {
            if (client.getUsername().equals(username)) {
                client.setPassword("");
                return client;
            }
        }
        return null;
    }

    //todo validate
    public boolean modifyClient(Client updatedClient) {
        // Ellenőrizni kell, hogy az ügyfél, amelyet frissíteni szeretnénk, megtalálható-e az adatbázisban
        Client existingClient = clientRepository.findById(updatedClient.getId())
                .orElseThrow(() -> new IllegalArgumentException("Az ügyfél nem található az adatbázisban"));

        // Ellenőrizni kell, hogy a felhasználónév nem módosítható
        if (!existingClient.getUsername().equals(updatedClient.getUsername())) {
            throw new IllegalArgumentException("A felhasználónév nem módosítható");
        }

        // A többi mezőt frissítjük a kapott értékekkel
        existingClient.setName(updatedClient.getName());
        existingClient.setPassword(updatedClient.getPassword());
        existingClient.setEmail(updatedClient.getEmail());

        // A frissített ügyfelet mentjük
        clientRepository.save(existingClient);
        return true;
    }
}
