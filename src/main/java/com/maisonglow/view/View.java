package com.maisonglow.view;

/**
 * Every screen of the application, with the text of its menu entry and the
 * classpath location of its {@code .fxml}. A screen whose file does not exist
 * yet is shown as "En construcción" by the {@link Navigator}.
 */
public enum View {
    HOME("Inicio", "HomeView.fxml"),
    CUSTOMERS("Clientes", "CustomersView.fxml"),
    PROFESSIONALS("Profesionales", "ProfessionalsView.fxml"),
    APPOINTMENTS("Citas", "AppointmentsView.fxml"),
    SALES("Ventas", "SalesView.fxml"),
    PRODUCTS("Productos", "ProductsView.fxml"),
    STATISTICS("Estadísticas", "StatisticsView.fxml"),
    ASSISTANT("Asistente IA", "AssistantView.fxml"),
    TRY_ON("Probador virtual", "TryOnView.fxml"),
    SETTINGS("Configuración", "SettingsView.fxml");

    private static final String BASE = "/com/maisonglow/view/";

    private final String title;
    private final String file;

    View(String title, String file) {
        this.title = title;
        this.file = file;
    }

    /** @return the text shown in the menu and in the placeholder */
    public String title() {
        return title;
    }

    /** @return the classpath location of the {@code .fxml} of this screen */
    public String resource() {
        return BASE + file;
    }
}
