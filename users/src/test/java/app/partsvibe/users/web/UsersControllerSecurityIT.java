package app.partsvibe.users.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.users.commands.usermanagement.CreateUserCommand;
import app.partsvibe.users.commands.usermanagement.DeleteUserCommand;
import app.partsvibe.users.commands.usermanagement.DeleteUserCommandResult;
import app.partsvibe.users.commands.usermanagement.UpdateUserCommand;
import app.partsvibe.users.models.UserDetailsModel;
import app.partsvibe.users.queries.usermanagement.SearchUsersQuery;
import app.partsvibe.users.queries.usermanagement.UserByIdQuery;
import app.partsvibe.users.test.web.AbstractUsersWebIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

class UsersControllerSecurityIT extends AbstractUsersWebIntegrationTest {
    @BeforeEach
    void setUpMediatorHandlers() {
        mediator.onQuery(SearchUsersQuery.class, query -> new PageResult<>(List.of(), 0, 0, 1, 10));
        mediator.onQuery(UserByIdQuery.class, query -> new UserDetailsModel(1L, "bob@example.com", true, List.of()));
        mediator.onCommand(
                CreateUserCommand.class, command -> new UserDetailsModel(2L, "new@example.com", true, List.of()));
        mediator.onCommand(
                UpdateUserCommand.class, command -> new UserDetailsModel(1L, "bob@example.com", true, List.of()));
        mediator.onCommand(DeleteUserCommand.class, command -> new DeleteUserCommandResult("bob@example.com"));
        mediator.onCommand(
                app.partsvibe.users.commands.admin.TriggerRetentionCleanupCommand.class,
                command ->
                        new app.partsvibe.users.commands.admin.TriggerRetentionCleanupCommandResult(UUID.randomUUID()));
    }

    @Test
    @WithMockUser(roles = "USER")
    void roleUserCannotAccessAdminUsersEndpoints() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/create")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/1")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/users/1/edit")).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin")).andExpect(status().isForbidden());

        mockMvc.perform(post("/admin/users/create").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/edit").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/do-delete").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/do-lock").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/users/1/do-unlock").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/event-queue/retention-cleanup").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void roleAdminCanAccessAdminUsersEndpoints() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/create")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/1")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/1/edit")).andExpect(status().isOk());
        mockMvc.perform(get("/admin")).andExpect(status().isOk());

        mockMvc.perform(post("/admin/users/create").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/edit").with(csrf()).param("username", "john@example.com"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/do-delete").with(csrf())).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/do-lock").with(csrf())).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/1/do-unlock").with(csrf())).andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/event-queue/retention-cleanup").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
