package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;

import java.util.Set;

/** Проверка связи с сервером. */
public class PingCommand extends AbstractCommand {

    @Override
    protected Set<Role> allowedRoles() {
        return ANYONE;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) {
        return Response.ok("pong", null);
    }
}
