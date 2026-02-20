package app.partsvibe.users.test.web;

import app.partsvibe.testsupport.web.WebTestBaseApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@ComponentScan(basePackages = "app.partsvibe.users.web")
@Import(WebTestBaseApplication.class)
public class UsersWebTestApplication {}
