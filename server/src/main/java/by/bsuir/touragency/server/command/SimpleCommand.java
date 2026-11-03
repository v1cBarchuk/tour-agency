package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;

import java.util.Set;

/**
 * Команда, логика которой задаётся лямбдой. Используется для простых
 * операций чтения и сохранения, чтобы не плодить однотипные классы.
 */
public class SimpleCommand extends AbstractCommand {

    @FunctionalInterface
    public interface Handler {
        Response handle(Request request, ClientSession session) throws AppException;
    }

    private final Set<Role> roles;
    private final Handler handler;

    public SimpleCommand(Set<Role> roles, Handler handler) {
        this.roles = roles;
        this.handler = handler;
    }

    @Override
    protected Set<Role> allowedRoles() {
        return roles;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) throws AppException {
        return handler.handle(request, session);
    }
}
