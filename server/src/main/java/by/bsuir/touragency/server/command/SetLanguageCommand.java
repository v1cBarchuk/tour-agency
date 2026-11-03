package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.service.AuthService;

/** Сохраняет выбранный язык интерфейса в профиле пользователя. */
public class SetLanguageCommand extends AbstractCommand {

    private final AuthService authService;

    public SetLanguageCommand(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) {
        authService.changeLanguage(session.getUser(), request.getLang());
        return Response.ok(msg(request, "auth.languageChanged"), null);
    }
}
