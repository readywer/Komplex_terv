package org.movie.web.controller;

import org.movie.persistance.ClientRepository;
import org.movie.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @Autowired
    private MovieService movieService;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    @GetMapping("/home")
    public String home() {


//        Client client1 = new Client();
//        client1.setEmail("alma@alma.hu");
//        client1.setRole(Role.ADMIN);
//        client1.setName("Dobai Attila");
//        client1.setUsername("readywer");
//        client1.setPassword("asd123");
//        Client savedClient = clientRepository.save(client1);
//        System.out.println("pass: " + clientRepository.findById(5L));
//        System.out.println("Username: " + savedClient.getUsername());
//        System.out.println("Name: " + savedClient.getName());


        return "home-page";
    }
}
