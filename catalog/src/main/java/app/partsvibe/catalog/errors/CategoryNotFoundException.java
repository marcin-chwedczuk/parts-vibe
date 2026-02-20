package app.partsvibe.catalog.errors;

import app.partsvibe.shared.error.ApplicationException;

public class CategoryNotFoundException extends ApplicationException {
    public CategoryNotFoundException(Long categoryId) {
        super("Category not found. categoryId=" + categoryId);
    }
}
