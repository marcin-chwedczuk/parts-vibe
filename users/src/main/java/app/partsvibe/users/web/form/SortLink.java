package app.partsvibe.users.web.form;

import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;

public record SortLink(String url, boolean active, String direction) {
    public boolean isAsc() {
        return active && SearchUsersQuery.SORT_ASC.equals(direction);
    }

    public boolean isDesc() {
        return active && SearchUsersQuery.SORT_DESC.equals(direction);
    }

    public boolean isNone() {
        return !active || SearchUsersQuery.SORT_NONE.equals(direction);
    }
}
