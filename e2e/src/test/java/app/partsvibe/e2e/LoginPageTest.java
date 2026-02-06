package app.partsvibe.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginPageTest extends BaseE2ETest {
    @Test
    void rendersLoginPage() {
        page.navigate(baseUrl() + "/login");

        String title = page.getByTestId("login-title").textContent();
        assertThat(title).isEqualTo("Login");
    }
}
