package app.partsvibe.config;

import org.springframework.stereotype.Component;

@Component("nav")
public class NavigationHelper {
    public boolean isUnder(String currentPath, String prefix) {
        if (currentPath == null || currentPath.isBlank() || prefix == null || prefix.isBlank()) {
            return false;
        }
        return currentPath.startsWith(prefix);
    }
}
