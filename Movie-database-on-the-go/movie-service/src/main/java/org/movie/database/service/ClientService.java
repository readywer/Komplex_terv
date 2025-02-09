package org.movie.database.service;

import org.movie.database.domain.Client;
import org.movie.database.persistence.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ClientService {
    @Autowired
    private FilmService filmService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public String createClient(Client client) {
        String result = null;
        if (isValidClient(client)) {
            encodePassword(client);
            try {
                createDirectory(client);
            } catch (IOException e) {
                result = "IO Error. Please try again";
                return result;
            }
            try {
                clientRepository.save(client);
            } catch (Exception e) {
                result = "Database Error. Please try again";
                return result;
            }
            return result;
        }
        result = "The username is already in use.";
        return result;
    }

    private void encodePassword(Client client) {
        String encodedPassword = passwordEncoder.encode(client.getPassword());
        client.setPassword(encodedPassword);
    }

    private void createDirectory(Client client) throws IOException {
        String folderPath = filmService.getStorageDir() + "/" + client.getUsername();
        Files.createDirectories(Paths.get(folderPath));
        Path filePath = Paths.get(folderPath, "film.json");
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }
    }

    public boolean isValidClient(Client client) {
        return isValidName(client.getName()) &&
                isValidUsername(client.getUsername()) &&
                isValidPassword(client.getPassword()) &&
                isValidEmail(client.getEmail());
    }

    public boolean isValidUsername(String username) {
        return username.length() >= 3 && username.length() <= 32 &&
                Character.isLetter(username.charAt(0)) &&
                clientRepository.findByUsername(username) == null &&
                username.matches("^[a-zA-Z0-9.,_-]+$");
    }

    public boolean isValidName(String name) {
        return name.length() >= 3 && name.length() <= 32 &&
                Character.isLetter(name.charAt(0));
    }

    public boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{6,}$");
    }

    public boolean isValidEmail(String email) {
        return !email.isEmpty() &&
                email.matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f])+)])");
    }

    public boolean modifyClient(Client updatedClient) {
        // Ellenőrizni kell, hogy az ügyfél, amelyet frissíteni szeretnénk, megtalálható-e az adatbázisban
        Client existingClient = clientRepository.findById(updatedClient.getId())
                .orElseThrow(() -> new IllegalArgumentException("Client not found."));
        if (updatedClient.getName().isEmpty() && updatedClient.getEmail().isEmpty() && updatedClient.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Empty input.");
        }

        if (!updatedClient.getName().isEmpty()) {
            if (isValidName(updatedClient.getName())) {
                existingClient.setName(updatedClient.getName());
            } else {
                return false;
            }
        }
        if (!updatedClient.getEmail().isEmpty()) {
            if (isValidEmail(updatedClient.getEmail())) {
                existingClient.setEmail(updatedClient.getEmail());
            } else {
                return false;
            }
        }
        if (!updatedClient.getPassword().isEmpty()) {
            if (isValidPassword(updatedClient.getPassword())) {
                encodePassword(updatedClient);
                existingClient.setPassword(updatedClient.getPassword());
            } else {
                return false;
            }
        }

        clientRepository.save(existingClient);
        return true;
    }
}
