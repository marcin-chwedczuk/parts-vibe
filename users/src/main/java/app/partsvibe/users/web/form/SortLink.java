package app.partsvibe.users.web.form;

public record SortLink(String url, boolean active, String direction) {
    public boolean isAsc() {
        return active && UserManagementFilters.SORT_ASC.equals(direction);
    }

    public boolean isDesc() {
        return active && UserManagementFilters.SORT_DESC.equals(direction);
    }

    public boolean isNone() {
        return !active || UserManagementFilters.SORT_NONE.equals(direction);
    }
}
