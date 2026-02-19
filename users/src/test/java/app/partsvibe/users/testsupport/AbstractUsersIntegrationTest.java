package app.partsvibe.users.testsupport;

import app.partsvibe.testsupport.AbstractPostgresIntegrationTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(classes = UsersModuleTestApplication.class)
@Transactional
public abstract class AbstractUsersIntegrationTest extends AbstractPostgresIntegrationTest {}
