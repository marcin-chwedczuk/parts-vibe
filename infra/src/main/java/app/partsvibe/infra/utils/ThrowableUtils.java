package app.partsvibe.infra.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CompletionException;

public final class ThrowableUtils {
    private ThrowableUtils() {}

    public static String asString(Throwable throwable) {
        if (throwable == null) {
            return "<null-throwable>";
        }

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
        }
        return stringWriter.toString().stripTrailing();
    }

    public static Throwable unwrapCompletionThrowable(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }
        return throwable;
    }

    public static String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "<null-throwable>";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "<no-message>";
        }
        return message;
    }
}
