package app.partsvibe.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

abstract class BaseE2ETest {
    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create();
        boolean headless = Boolean.parseBoolean(System.getProperty("e2e.headless", "true"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    @AfterAll
    static void stopBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    protected String baseUrl() {
        String fromProperty = System.getProperty("e2e.baseUrl");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        String fromEnv = System.getenv("E2E_BASE_URL");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return "http://localhost:8080";
    }
}
