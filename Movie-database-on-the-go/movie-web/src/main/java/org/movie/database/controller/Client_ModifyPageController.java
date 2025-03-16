package org.movie.database.controller;

import jakarta.validation.Valid;
import org.movie.database.domain.Client;
import org.movie.database.persistence.ClientRepository;
import org.movie.database.security.UserLoginDetailsService;
import org.movie.database.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class Client_ModifyPageController {
    @Autowired
    private ClientService clientService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserLoginDetailsService userLoginDetailsService;

    @GetMapping("/client_modify")
    public String clientModify(Model model) {
        Client client = clientRepository.findByUsername(userLoginDetailsService.loadAuthenticatedUsername());
        client.setPassword("");
        model.addAttribute("client", client);
        return "client_modify-page";
    }

    @GetMapping("/admin/client_modify")
    public String clientModifyAdmin(@RequestParam(name = "clientId") Long clientId, Model model) {
        Client client = clientRepository.findById(clientId).orElse(null);
        if (client == null) {
            return "error/404"; // Ha nincs ilyen ügyfél, akkor 404 oldal
        }
        client.setPassword("");
        model.addAttribute("client", client);
        return "client_modify-page";
    }

    @PostMapping("/client_modify")
    public String modify(@ModelAttribute("client") @Valid Client client, Model model) {
        client.setId(clientRepository.findByUsername(userLoginDetailsService.loadAuthenticatedUsername()).getId());
        if (clientService.modifyClient(client)) {
            return "redirect:/home";
        }
        model.addAttribute("client", client);
        return "client_modify-page";
    }

    @PostMapping("/admin/client_modify")
    public String modifyAdmin(@ModelAttribute("client") @Valid Client client, Model model) {
        if (clientService.modifyClient(client)) {
            return "redirect:/home";
        }
        model.addAttribute("client", client);
        return "client_modify-page";
    }
}
