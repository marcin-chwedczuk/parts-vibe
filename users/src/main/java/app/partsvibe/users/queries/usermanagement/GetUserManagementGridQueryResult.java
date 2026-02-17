package app.partsvibe.users.queries.usermanagement;

import java.util.List;

public record GetUserManagementGridQueryResult(
        List<UserRow> rows, long totalRows, int totalPages, int currentPage, int startRow, int endRow) {
    public record UserRow(Long id, String username, boolean enabled, List<String> roles) {}
}
