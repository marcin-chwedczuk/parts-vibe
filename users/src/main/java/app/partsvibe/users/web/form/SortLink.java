package app.partsvibe.users.web.form;

import app.partsvibe.users.queries.usermanagement.GetUserManagementGridQuery;

public record SortLink(String url, boolean active, String direction) {
    public boolean isAsc() {
        return active && GetUserManagementGridQuery.SORT_ASC.equals(direction);
    }

    public boolean isDesc() {
        return active && GetUserManagementGridQuery.SORT_DESC.equals(direction);
    }

    public boolean isNone() {
        return !active || GetUserManagementGridQuery.SORT_NONE.equals(direction);
    }
}
