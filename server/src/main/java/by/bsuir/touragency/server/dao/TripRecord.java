package by.bsuir.touragency.server.dao;

/** Завершённая поездка клиента: куда ездил, какой тип отдыха и как оценил. */
public record TripRecord(int countryId, String countryName, int vacationTypeId, Integer rating) {
}
