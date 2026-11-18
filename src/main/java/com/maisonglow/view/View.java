package com.maisonglow.view;

import com.maisonglow.view.access.AccessRole;

/**
 * Every screen of the application, with the text of its menu entry and the
 * classpath location of its {@code .fxml}. A screen whose file does not exist
 * yet is shown as "En construcción" by the {@link Navigator}.
 */
public enum View {
    HOME("Inicio", "HomeView.fxml", false, AccessRole.ADMIN),
    CUSTOMERS("Clientes", "CustomersView.fxml", false, AccessRole.ADMIN),
    PROFESSIONALS("Profesionales", "ProfessionalsView.fxml", false, AccessRole.ADMIN),
    APPOINTMENTS("Citas", "AppointmentsView.fxml", false, AccessRole.ADMIN),
    SALES("Ventas", "SalesView.fxml", false, AccessRole.ADMIN),
    ORDERS("Pedidos", "OrdersView.fxml", false, AccessRole.ADMIN),
    PRODUCTS("Productos", "ProductsView.fxml", false, AccessRole.ADMIN),
    STATISTICS("Estadísticas", "StatisticsView.fxml", false, AccessRole.ADMIN),
    ASSISTANT("Asistente IA", "AssistantView.fxml", true, AccessRole.ADMIN),
    TRY_ON("Probador virtual", "TryOnView.fxml", true, AccessRole.ADMIN),
    SETTINGS("Configuración", "SettingsView.fxml", false, AccessRole.ADMIN),

    // The area of the customer. These routes are ready for the next block of the roadmap (N-R01 to N-R05);
    // until each screen exists the Navigator shows "En construcción".
    CUSTOMER_HOME("Inicio", "CustomerHomeView.fxml", false, AccessRole.CUSTOMER),
    PROFILE("Mi perfil", "ProfileView.fxml", false, AccessRole.CUSTOMER),
    SHOP("Tienda", "ShopView.fxml", false, AccessRole.CUSTOMER),
    MY_PURCHASES("Mis compras", "MyPurchasesView.fxml", false, AccessRole.CUSTOMER),
    MY_APPOINTMENTS("Mis citas", "MyAppointmentsView.fxml", false, AccessRole.CUSTOMER),
    MY_CHANGES("Mis cambios", "MyChangesView.fxml", true, AccessRole.CUSTOMER);

    private static final String BASE = "/com/maisonglow/view/";

    private final String title;
    private final String file;
    private final boolean keepsState;
    private final AccessRole role;

    View(String title, String file, boolean keepsState, AccessRole role) {
        this.title = title;
        this.file = file;
        this.keepsState = keepsState;
        this.role = role;
    }

    /**
     * @param who the role of the session
     * @return true if that role may open this screen; the menu hides the others and the {@link Navigator} refuses them
     */
    public boolean allowedFor(AccessRole who) {
        return role == who;
    }

    /**
     * @param who the role of the session
     * @return the screen a person of that role sees first
     */
    public static View startFor(AccessRole who) {
        return who == AccessRole.ADMIN ? HOME : CUSTOMER_HOME;
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
