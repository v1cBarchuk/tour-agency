package by.bsuir.touragency.server.dao;

import by.bsuir.touragency.common.model.Discount;

import java.util.List;
import java.util.Optional;

public interface DiscountDao {

    List<Discount> findAll();

    List<Discount> findActive();

    Optional<Discount> findById(int id);

    int save(Discount discount);

    void setActive(int id, boolean active);
}
