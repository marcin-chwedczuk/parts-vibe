package app.partsvibe.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.springframework.stereotype.Component;

@Component
public class BuildVersionInfo {
    private static final String RESOURCE_NAME = "version-info.properties";
    private static final String UNKNOWN = "unknown";

    private final String buildTime;
    private final String mavenVersion;
    private final String jdkVersion;
    private final String gitCommitIdFull;
    private final String gitCommitIdShort;
    private final String gitClosestTagName;
    private final String gitClosestTagCommitCount;

    public BuildVersionInfo() {
        Properties properties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(RESOURCE_NAME)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load " + RESOURCE_NAME, e);
        }

        this.buildTime = valueOf(properties, "build.time");
        this.mavenVersion = valueOf(properties, "maven.version");
        this.jdkVersion = valueOf(properties, "jdk.version");
        this.gitCommitIdFull = valueOf(properties, "git.commit.id.full");
        this.gitCommitIdShort = valueOf(properties, "git.commit.id.short");
        this.gitClosestTagName = valueOf(properties, "git.closest.tag.name");
        this.gitClosestTagCommitCount = valueOf(properties, "git.closest.tag.commit.count");
    }

    public String buildTime() {
        return buildTime;
    }

    public String mavenVersion() {
        return mavenVersion;
    }

    public String jdkVersion() {
        return jdkVersion;
    }

    public String gitCommitIdFull() {
        return gitCommitIdFull;
    }

    public String gitCommitIdShort() {
        return gitCommitIdShort;
    }

    public String gitClosestTagName() {
        return gitClosestTagName;
    }

    public int gitCommitsSinceClosestTag() {
        try {
            return Integer.parseInt(gitClosestTagCommitCount);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String gitVersionLabel() {
        int commitsSinceTag = gitCommitsSinceClosestTag();
        if (UNKNOWN.equals(gitClosestTagName)) {
            return gitCommitIdShort;
        }
        if (commitsSinceTag <= 0) {
            return gitClosestTagName;
        }
        return gitClosestTagName + "+" + commitsSinceTag;
    }

    private static String valueOf(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            return UNKNOWN;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || (trimmed.startsWith("${") && trimmed.endsWith("}"))) {
            return UNKNOWN;
        }
        return trimmed;
    }
}
