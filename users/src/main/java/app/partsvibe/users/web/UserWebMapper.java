package app.partsvibe.users.web;

import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;
import app.partsvibe.users.web.form.UserGridRow;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserWebMapper {
    UserGridRow toGridRow(SearchUsersQuery.User source);

    List<UserGridRow> toGridRows(List<SearchUsersQuery.User> source);
}
