package by.bsuir.touragency.client.view;

/**
 * Навигация внутри главного окна, доступная разделам.
 */
public interface Shell {

    /** Открыть страницу поверх текущей (с кнопкой «Назад»). */
    void open(BaseView page);

    /** Вернуться на предыдущую страницу. */
    void back();

    /** Перейти к разделу бокового меню по ключу (menu.*). */
    void openSection(String key);

    // Перегрузка: раздел с уже подготовленной страницей (например, каталог с фильтром)
    void openSection(String key, BaseView page);

    /** Обновить текущую страницу (F5). */
    default void refresh() {
    }
}
