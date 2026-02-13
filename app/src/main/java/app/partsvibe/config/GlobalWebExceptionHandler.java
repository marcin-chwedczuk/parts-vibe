package app.partsvibe.config;

import app.partsvibe.shared.error.BusinessAccessDeniedException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalWebExceptionHandler {
    @ExceptionHandler(BusinessAccessDeniedException.class)
    public String handleBusinessAccessDenied(BusinessAccessDeniedException ignored, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        return "error/403";
    }
}
