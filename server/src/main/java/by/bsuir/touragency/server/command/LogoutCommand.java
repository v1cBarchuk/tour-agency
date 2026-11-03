package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.service.AuthService;

public class LogoutCommand extends AbstractCommand {

    private final AuthService authService;

    public LogoutCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) {
        authService.logout(session.getUser());
        session.clear();
        return Response.ok(msg(request, "auth.loggedOut"), null);
    }
}
