package app.partsvibe.testsupport.web;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@ComponentScan(basePackages = "app.partsvibe.users.web")
@Import(WebTestBaseApplication.class)
public class UsersWebModuleTestApplication {}
