package org.movie.service;

import org.movie.domain.Client;
import org.movie.persistance.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@Service
public class MovieService {
    @Autowired
    private ClientRepository clientRepository;

    public MovieService() {

    }

    @Transactional
    public boolean createClient(Client client) {
        System.out.println("eljut");

            System.out.println("siker2");
            createDirectory(client);
            clientRepository.save(client);
            return true;

    }

    public void createDirectory(Client client) {
        String folderPath = "data/" + client.getUsername();

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

    public boolean isValidClient(Client client) {
        return client.getName() != null && !client.getName().isEmpty() &&
                client.getUsername() != null && !client.getUsername().isEmpty() &&
                client.getPassword() != null && !client.getPassword().isEmpty() &&
                client.getEmail() != null && !client.getEmail().isEmpty() &&
                client.getRole() != null;
    }

    public Client findClientByUsername(String clientname) {
        for (Client client : clientRepository.findAll()) {
            if (client.getUsername().equals(clientname)) {
                return client;
            }
        }
        return null;
    }
}
