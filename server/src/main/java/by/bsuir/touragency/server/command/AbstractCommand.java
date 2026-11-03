package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.common.protocol.ResponseStatus;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.util.ServerMessages;

import java.util.EnumSet;
import java.util.Set;

/**
 * Шаблонный метод: общая проверка прав, затем логика конкретной команды.
 * Наследники переопределяют allowedRoles() и реализуют doExecute().
 */
public abstract class AbstractCommand implements Command {

    /** Доступно без входа. */
    protected static final Set<Role> ANYONE = EnumSet.noneOf(Role.class);
    /** Любой авторизованный пользователь. */
    protected static final Set<Role> AUTHENTICATED = EnumSet.allOf(Role.class);
    /** Сотрудники: менеджер и администратор. */
    protected static final Set<Role> STAFF = EnumSet.of(Role.MANAGER, Role.ADMIN);
    /** Только клиент. */
    protected static final Set<Role> CLIENTS = EnumSet.of(Role.CLIENT);
    /** Только администратор. */
    protected static final Set<Role> ADMINS = EnumSet.of(Role.ADMIN);

    /** Роли, которым разрешена команда. Пустой набор — доступна без входа. */
    protected Set<Role> allowedRoles() {
        return AUTHENTICATED;
    }

    @Override
    public final Response execute(Request request, ClientSession session) throws AppException {
        Set<Role> roles = allowedRoles();
        if (!roles.isEmpty()) {
            if (!session.isAuthenticated()) {
                return Response.of(ResponseStatus.UNAUTHORIZED, msg(request, "error.unauthorized"));
            }
            if (!roles.contains(session.getUser().getRole())) {
                return Response.of(ResponseStatus.FORBIDDEN, msg(request, "error.forbidden"));
            }
        }
        return doExecute(request, session);
    }

    /** Текст сообщения на языке клиента. */
    protected static String msg(Request request, String key, Object... args) {
        return ServerMessages.get(request.getLang(), key, args);
    }

    protected abstract Response doExecute(Request request, ClientSession session) throws AppException;
}
