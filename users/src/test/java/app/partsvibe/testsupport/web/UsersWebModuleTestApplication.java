package app.partsvibe.testsupport.web;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@ComponentScan(basePackages = "app.partsvibe.users.web")
@Import(UsersWebBaseTestApplication.class)
public class UsersWebModuleTestApplication {}
