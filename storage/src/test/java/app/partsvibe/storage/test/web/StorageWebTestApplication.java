package app.partsvibe.storage.test.web;

import app.partsvibe.testsupport.web.WebTestBaseApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@ComponentScan(basePackages = "app.partsvibe.storage.web")
@Import(WebTestBaseApplication.class)
public class StorageWebTestApplication {}
