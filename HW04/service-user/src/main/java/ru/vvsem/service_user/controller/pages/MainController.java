package ru.vvsem.service_user.controller.pages;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    // Редирект с базового пусти / на сваггер
    @GetMapping("/")
    public String redirect() {
        return "redirect:/swagger-ui.html";
    }
}
