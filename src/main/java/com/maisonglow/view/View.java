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
    ASSISTANT("Asistente IA", "AssistantView.fxml", true),
    TRY_ON("Probador virtual", "TryOnView.fxml", true),
    SETTINGS("Configuración", "SettingsView.fxml");

    private static final String BASE = "/com/maisonglow/view/";

    private final String title;
    private final String file;
    private final boolean keepsState;

    View(String title, String file) {
        this(title, file, false);
    }

    View(String title, String file, boolean keepsState) {
        this.title = title;
        this.file = file;
        this.keepsState = keepsState;
    }

    /** @return true if the screen is kept alive when the user leaves it, so a chat or a photo is still there on return */
    public boolean keepsState() {
        return keepsState;
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
