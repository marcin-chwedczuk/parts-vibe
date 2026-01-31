package app.partsvibe.users.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/login")
    public String login() {
        log.info("Login page requested");
        return "login";
    }
}
