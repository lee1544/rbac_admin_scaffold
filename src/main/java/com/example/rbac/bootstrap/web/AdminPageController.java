package com.example.rbac.bootstrap.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves as the entry point for the bundled static admin console.
 * The console itself authenticates against the REST API with JWT.
 */
@Controller
public class AdminPageController {

    @GetMapping({"/", "/admin", "/admin/"})
    public String admin() {
        return "redirect:/admin/login.html";
    }
}
