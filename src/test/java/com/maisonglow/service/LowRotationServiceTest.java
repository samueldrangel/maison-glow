package com.maisonglow.service;

import com.maisonglow.dao.CrudDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sellable;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LowRotationService tests")
class LowRotationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);
    private static final LocalDate WINDOW_START = LocalDate.of(2026, 10, 2);

    @Mock
    private SaleDao saleDao;
    @Mock
    private CrudDao<Product> productDao;
    @Mock
    private SettingsService settings;

    private LowRotationService service;

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final ProductCategory hair = new ProductCategory(1, "Cabello");
    private final Product shampoo = new Product(1, "Shampoo", hair, 28000, 20, 2);
    private final Product mask = new Product(2, "Mascarilla", hair, 35000, 20, 2);
    private final Product serum = new Product(3, "Sérum", hair, 52000, 20, 2);
    private final Product oil = new Product(4, "Aceite", hair, 40000, 20, 2);

    @BeforeEach
    void setUp() throws Exception {
        service = new LowRotationService(saleDao, productDao, settings);
        lenient().when(settings.lowRotationWindowDays()).thenReturn(30);
        lenient().when(settings.lowRotationTopN()).thenReturn(3);
        lenient().when(productDao.findAll()).thenReturn(List.of(shampoo, mask, serum, oil));
    }

    private Sale sale(int id, Sellable item, int quantity) throws Exception {
        return Sale.restore(id, ana, LocalDateTime.of(2026, 10, 20, 10, 0),
            List.of(SaleLine.restore(item, quantity, item.getPrice())));
    }

    @Test
    @DisplayName("lowRotationProducts lists the least sold first, including products with no sales")
    void lowestFirst() throws Exception {
        when(saleDao.findBetween(WINDOW_START, TODAY)).thenReturn(List.of(sale(1, shampoo, 10), sale(2, mask, 2),
            sale(3, serum, 1), sale(4, mask, 1)));

        List<ProductSales> products = service.lowRotationProducts(TODAY);

        assertThat(products).extracting(sales -> sales.product().getName())
            .containsExactly("Aceite", "Sérum", "Mascarilla");
        assertThat(products).extracting(ProductSales::unitsSold).containsExactly(0, 1, 3);
    }

    @Test
    @DisplayName("the window and the number of products come from the settings")
    void usesSettings() throws Exception {
        when(settings.lowRotationWindowDays()).thenReturn(7);
        when(settings.lowRotationTopN()).thenReturn(1);
        when(saleDao.findBetween(LocalDate.of(2026, 10, 25), TODAY)).thenReturn(List.of(sale(1, oil, 3)));

        assertThat(service.lowRotationProducts(TODAY)).extracting(sales -> sales.product().getName())
            .containsExactly("Mascarilla");
    }

    @Test
    @DisplayName("inactive products and treatments are left out")
    void skipsInactiveAndTreatments() throws Exception {
        oil.deactivate();
        Treatment haircut = new Treatment(1, "Corte de cabello", "Corte y secado", 35000, 45);
        when(saleDao.findBetween(WINDOW_START, TODAY)).thenReturn(List.of(sale(1, haircut, 1)));

        assertThat(service.lowRotationProducts(TODAY)).extracting(sales -> sales.product().getName())
            .containsExactly("Mascarilla", "Shampoo", "Sérum");
    }

    @Test
    @DisplayName("notifyAdministrator sends the report through the notifier")
    void notifyAdministrator() throws Exception {
        when(saleDao.findBetween(WINDOW_START, TODAY)).thenReturn(List.of(sale(1, shampoo, 4)));
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        SimulatedAdminNotifier notifier = new SimulatedAdminNotifier(
            new PrintStream(printed, true, StandardCharsets.UTF_8));

        List<ProductSales> reported = service.notifyAdministrator(notifier, TODAY);

        assertThat(reported).hasSize(3);
        assertThat(notifier.getNotifications()).singleElement().satisfies(message -> {
            assertThat(message).startsWith("Productos de baja rotación en los últimos 30 días:");
            assertThat(message).contains("- Aceite: 0 und. vendidas").doesNotContain("Shampoo");
        });
        assertThat(printed.toString(StandardCharsets.UTF_8)).contains("[Aviso al administrador]");
    }

    @Test
    @DisplayName("the simulated notifier says when there is nothing to report")
    void emptyReport() {
        SimulatedAdminNotifier notifier = new SimulatedAdminNotifier(
            new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));

        notifier.notifyLowRotation(List.of(), 30);

        assertThat(notifier.getNotifications().get(0)).endsWith("(ninguno)");
    }

    @Test
    @DisplayName("a missing date or an invalid setting is rejected")
    void invalidInput() throws Exception {
        assertThatThrownBy(() -> service.lowRotationProducts(null)).isInstanceOf(ValidationException.class);

        when(settings.lowRotationWindowDays()).thenThrow(new ValidationException("valor inválido"));
        assertThatThrownBy(() -> service.lowRotationProducts(TODAY)).isInstanceOf(ValidationException.class);
    }
}
