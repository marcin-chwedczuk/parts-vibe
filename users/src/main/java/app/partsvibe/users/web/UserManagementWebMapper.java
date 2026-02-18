package app.partsvibe.users.web;

import app.partsvibe.users.queries.usermanagement.GetUserManagementGridQuery;
import app.partsvibe.users.web.form.UserGridRow;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserManagementWebMapper {
    UserGridRow toGridRow(GetUserManagementGridQuery.User source);

    List<UserGridRow> toGridRows(List<GetUserManagementGridQuery.User> source);
}
