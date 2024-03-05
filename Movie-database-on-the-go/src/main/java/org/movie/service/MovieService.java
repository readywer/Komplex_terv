package org.movie.service;

import org.movie.domain.Client;
import org.movie.persistance.ClientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MovieService {
    @Autowired
    private ClientRepository clientRepository;

    public void createClient(Client client){
        if (isValidClient(client)){
            if(!clientRepository.findByUsername(client.getUsername()).equals("") && clientRepository.findByUsername(client.getUsername()) !=null){
                clientRepository.save(client);
            }
        }
        else {
            throw new IllegalArgumentException("A játékos hibás megadása.");
        }
    }

    public boolean isValidClient(Client client) {
        return client.getName() != null && !client.getName().isEmpty() &&
                client.getUsername() != null && !client.getUsername().isEmpty() &&
                client.getPassword() != null && !client.getPassword().isEmpty() &&
                client.getEmail() != null && !client.getEmail().isEmpty() &&
                client.getRole() != null;
    }
}
