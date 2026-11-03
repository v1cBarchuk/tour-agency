package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.User;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.service.AuthService;

import java.util.Set;

public class RegisterCommand extends AbstractCommand {

    private final AuthService authService;

    public RegisterCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected Set<Role> allowedRoles() {
        return ANYONE;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) throws AppException {
        User user = authService.registerClient(
                request.getString("login"),
                request.getString("password"),
                request.getString("fullName"),
                request.getString("email"),
                request.getString("phone", ""),
                request.getLang());
        return Response.ok(msg(request, "auth.registered"), user);
    }
}
