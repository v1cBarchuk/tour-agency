package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.exception.AppException;
import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.service.CatalogService;

import java.util.Set;

/** Удаление тура с проверкой зависимостей (бронирований). */
public class DeleteTourCommand extends AbstractCommand {

    private final CatalogService catalogService;

    public DeleteTourCommand(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    protected Set<Role> allowedRoles() {
        return STAFF;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) throws AppException {
        catalogService.deleteTour(request.getInt("tourId"), session.getUser(), request.getLang());
        return Response.ok(msg(request, "catalog.tourDeleted"), null);
    }
}
