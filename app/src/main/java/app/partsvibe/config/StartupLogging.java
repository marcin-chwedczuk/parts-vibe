package app.partsvibe.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupLogging implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(StartupLogging.class);
    private final Environment environment;
    private final BuildVersionInfo buildVersionInfo;

    public StartupLogging(Environment environment, BuildVersionInfo buildVersionInfo) {
        this.environment = environment;
        this.buildVersionInfo = buildVersionInfo;
    }

    @Override
    public void run(ApplicationArguments args) {
        String javaVersion = System.getProperty("java.version");
        String springBootVersion = SpringBootVersion.getVersion();
        String profiles = Arrays.stream(environment.getActiveProfiles()).collect(Collectors.joining(", "));
        if (profiles.isBlank()) {
            profiles = "default";
        }

        logger.info(
                "Startup: javaVersion={}, springBootVersion={}, profiles={}", javaVersion, springBootVersion, profiles);
        logger.info(
                "Build: buildTime={}, mavenVersion={}, jdkVersion={}, gitCommitIdShort={}, gitCommitIdFull={}",
                buildVersionInfo.buildTime(),
                buildVersionInfo.mavenVersion(),
                buildVersionInfo.jdkVersion(),
                buildVersionInfo.gitCommitIdShort(),
                buildVersionInfo.gitCommitIdFull());
    }
}
