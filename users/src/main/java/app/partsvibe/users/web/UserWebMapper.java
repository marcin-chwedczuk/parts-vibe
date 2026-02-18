package app.partsvibe.users.web;

import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;
import app.partsvibe.users.web.form.UserRow;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserWebMapper {
    UserRow toRow(SearchUsersQuery.UserRow source);

    List<UserRow> toRows(List<SearchUsersQuery.UserRow> source);
}
