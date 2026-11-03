package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.service.CatalogService;

import java.util.Set;
import java.util.function.Consumer;

/**
 * Создание и изменение тура менеджером. После создания нового тура
 * вызывается обработчик onTourCreated — к нему подключаются уведомления клиентов.
 */
public class SaveTourCommand extends AbstractCommand {

    private final CatalogService catalogService;
    private final Consumer<Tour> onTourCreated;

    public SaveTourCommand(CatalogService catalogService, Consumer<Tour> onTourCreated) {
        this.catalogService = catalogService;
        this.onTourCreated = onTourCreated;
    }

    @Override
    protected Set<Role> allowedRoles() {
        return STAFF;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) throws AppException {
        Tour tour = request.getObject("tour");
        boolean isNew = tour.getId() == null;
        Tour saved = catalogService.saveTour(tour, session.getUser(), request.getLang());
        if (isNew) {
            onTourCreated.accept(saved);
        }
        return Response.ok(msg(request, "catalog.saved"), saved);
    }
}
