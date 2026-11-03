package by.bsuir.touragency.server.dao;

import java.util.LinkedHashSet;
import java.util.Set;

public class JdbcFavoriteDao extends BaseDao implements FavoriteDao {

    @Override
    public boolean toggle(int userId, int tourId) {
        int removed = update("DELETE FROM favorites WHERE user_id = ? AND tour_id = ?", userId, tourId);
        if (removed > 0) {
            return false;
        }
        update("INSERT INTO favorites (user_id, tour_id) VALUES (?, ?)", userId, tourId);
        return true;
    }

    @Override
    public Set<Integer> findTourIds(int userId) {
        return new LinkedHashSet<>(query("SELECT tour_id FROM favorites WHERE user_id = ? ORDER BY added_at DESC",
                rs -> rs.getInt(1), userId));
    }
}
