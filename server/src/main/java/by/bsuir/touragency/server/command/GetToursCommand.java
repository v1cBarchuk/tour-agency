package by.bsuir.touragency.server.command;

import by.bsuir.touragency.common.model.Role;
import by.bsuir.touragency.common.model.Tour;
import by.bsuir.touragency.common.model.TourFilter;
import by.bsuir.touragency.common.protocol.Request;
import by.bsuir.touragency.common.protocol.Response;
import by.bsuir.touragency.server.network.ClientSession;
import by.bsuir.touragency.server.service.CatalogService;
import by.bsuir.touragency.server.service.RecommendationService;

import java.util.ArrayList;
import java.util.List;

/**
 * Поиск туров по фильтру. Клиенту показываются только доступные к продаже туры
 * с процентом совпадения с его анкетой и отметкой «в избранном»;
 * сотрудники могут видеть все туры (снятые с продажи и прошедшие).
 */
public class GetToursCommand extends AbstractCommand {

    private final CatalogService catalogService;
    private final RecommendationService recommendations;

    public GetToursCommand(CatalogService catalogService, RecommendationService recommendations) {
        this.catalogService = catalogService;
        this.recommendations = recommendations;
    }

    @Override
    protected Response doExecute(Request request, ClientSession session) {
        TourFilter filter = request.getObject("filter");
        if (filter == null) {
            filter = new TourFilter();
        }
        boolean client = session.getUser().getRole() == Role.CLIENT;
        if (client) {
            filter.setOnlyAvailable(true);
        }
        boolean byMatch = filter.getSort() == TourFilter.Sort.MATCH;
        if (byMatch) {
            filter.setSort(TourFilter.Sort.DATE);
        }
        List<Tour> tours = catalogService.getTours(filter, request.getLang());
        if (client) {
            tours = recommendations.annotate(new ArrayList<>(tours), session.getUser().getId(),
                    request.getLang(), byMatch);
        }
        return Response.ok(new ArrayList<>(tours));
    }
}
