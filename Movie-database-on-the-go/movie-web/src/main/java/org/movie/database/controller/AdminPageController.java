package org.movie.database.controller;

import org.movie.database.domain.Client;
import org.movie.database.domain.Film;
import org.movie.database.persistence.ClientRepository;
import org.movie.database.service.ClientService;
import org.movie.database.service.FilmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AdminPageController {
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private FilmService filmService;
    @Autowired
    private ClientService clientService;

    @GetMapping("/admin")
    public String admin(Model model) {
        List<Client> clients = (List<Client>) clientRepository.findAll();
        model.addAttribute("clients", clients);
        return "admin-page";
    }

    @GetMapping("/admin/client_details")
    public String filmDetailsAdmin(@RequestParam(name = "clientId") Long clientId, Model model) {
        Client client = clientRepository.findById(clientId).orElse(null);
        List<Film> films = filmService.getClientFilms(client.getUsername());
        model.addAttribute("usableSpace", filmService.formatStorageSize(filmService.getBytes()));
        model.addAttribute("usedSpace", filmService.formatStorageSize(filmService.getTotalStorageUsed(client.getUsername())));
        model.addAttribute("numberOfFilms", filmService.getClientFilms(client.getUsername()).size());
        model.addAttribute("films", films);
        model.addAttribute("client", client);
        return "client_details-page";
    }

    @PostMapping("/admin/client_delete")
    public String deleteClient(@RequestParam(name = "clientId") Long clientId) {
        clientService.deleteClient(clientId);
        return "redirect:/admin";
    }

    @PostMapping("/admin/client_add_admin_role")
    public String ClientAddAdminRole(@RequestParam(name = "clientId") Long clientId) {
        clientService.grantAdminRole(clientId);
        return "redirect:/admin";
    }

    @PostMapping("/admin/client_revoke_admin_role")
    public String ClientRevokeAdminRole(@RequestParam(name = "clientId") Long clientId) {
        clientService.revokeAdminRole(clientId);
        return "redirect:/admin";
    }
}
