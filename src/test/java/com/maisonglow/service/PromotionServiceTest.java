package com.maisonglow.service;

import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionService tests")
class PromotionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 31);

    @Mock
    private LowRotationService lowRotation;
    @Mock
    private SaleDao saleDao;
    @Mock
    private SettingsService settings;

    private PromotionService service;

    private final Customer ana = new Customer(1, "1001", "Ana Ruiz", "3001112222", "ana@correo.com");
    private final ProductCategory hair = new ProductCategory(1, "Cabello");
    private final Product serum = new Product(3, "Sérum", hair, 52000, 20, 2);
    private final Product oil = new Product(4, "Aceite", hair, 40000, 20, 2);

    @BeforeEach
    void setUp() throws Exception {
        service = new PromotionService(lowRotation, saleDao, settings);
        lenient().when(settings.lowRotationDiscountPercentage()).thenReturn(15.0);
        lenient().when(lowRotation.lowRotationProducts(TODAY))
            .thenReturn(List.of(new ProductSales(oil, 0), new ProductSales(serum, 1)));
    }

    private Sale saleOf(Product product) throws Exception {
        return Sale.restore(1, ana, LocalDateTime.of(2026, 9, 1, 10, 0),
            List.of(SaleLine.restore(product, 1, product.getPrice())));
    }

    @Test
    @DisplayName("suggests the configured discount on every low-rotation product")
    void suggestsDiscount() throws Exception {
        when(saleDao.findByCustomer(1)).thenReturn(List.of());

        List<PromotionSuggestion> suggestions = service.suggestPromotions(1, TODAY);

        assertThat(suggestions).containsExactly(new PromotionSuggestion("Aceite", 40000, 15, 34000),
            new PromotionSuggestion("Sérum", 52000, 15, 44200));
    }

    @Test
    @DisplayName("leaves out the products the customer already bought")
    void skipsBoughtProducts() throws Exception {
        when(saleDao.findByCustomer(1)).thenReturn(List.of(saleOf(oil)));

        assertThat(service.suggestPromotions(1, TODAY)).extracting(PromotionSuggestion::productName)
            .containsExactly("Sérum");
    }

    @Test
    @DisplayName("buying a treatment does not hide a product with the same id")
    void treatmentsDoNotCount() throws Exception {
        Treatment sameId = new Treatment(4, "Masaje", "Relajante", 60000, 60);
        when(saleDao.findByCustomer(1)).thenReturn(List.of(Sale.restore(1, ana, LocalDateTime.of(2026, 9, 1, 10, 0),
            List.of(SaleLine.restore(sameId, 1, 60000)))));

        assertThat(service.suggestPromotions(1, TODAY)).hasSize(2);
    }

    @Test
    @DisplayName("returns nothing when the customer already bought every low-rotation product")
    void nothingNew() throws Exception {
        when(saleDao.findByCustomer(1)).thenReturn(List.of(saleOf(oil), saleOf(serum)));

        assertThat(service.suggestPromotions(1, TODAY)).isEmpty();
    }

    @Test
    @DisplayName("an invalid discount setting is reported")
    void invalidDiscount() throws Exception {
        when(saleDao.findByCustomer(1)).thenReturn(List.of());
        when(settings.lowRotationDiscountPercentage()).thenThrow(new ValidationException("valor inválido"));

        assertThatThrownBy(() -> service.suggestPromotions(1, TODAY)).isInstanceOf(ValidationException.class);
    }
}
