package com.maisonglow.service;

import com.maisonglow.dao.ProductDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Sale;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SaleService tests")
class SaleServiceTest {

    private static final ProductCategory HAIR = new ProductCategory(1, "Cabello");

    @Mock
    private SaleDao saleDao;

    @Mock
    private ProductDao productDao;

    private SaleService service;
    private Customer customer;
    private Product shampoo;
    private Product mask;
    private Treatment facial;

    @BeforeEach
    void setUp() {
        service = new SaleService(saleDao, productDao);
        customer = new Customer(1, "100200300", "Ana Pérez", "3001234567", "ana@correo.com");
        shampoo = new Product(1, "Shampoo", HAIR, 28000, 10, 3);
        mask = new Product(2, "Mascarilla", HAIR, 35000, 4, 2);
        facial = new Treatment(1, "Limpieza facial", "Limpieza profunda", 80000, 60);
    }

    @Test
    @DisplayName("register deducts the stock of every product and stores the sale")
    void registerDeductsStockAndStores() throws Exception {
        Sale sale = new Sale(customer);
        sale.addLine(shampoo, 3);
        sale.addLine(mask, 4);
        when(saleDao.create(sale)).thenReturn(sale);

        Sale stored = service.register(sale);

        assertThat(stored).isSameAs(sale);
        assertThat(shampoo.getStock()).isEqualTo(7);
        assertThat(mask.getStock()).isZero();
        verify(productDao).update(shampoo);
        verify(productDao).update(mask);
        verify(saleDao).create(sale);
    }

    @Test
    @DisplayName("register leaves the stock of a treatment line alone and does not save it as a product")
    void registerSellsTreatmentWithoutStock() throws Exception {
        Sale sale = new Sale(customer);
        sale.addLine(facial, 1);
        sale.addLine(shampoo, 2);
        when(saleDao.create(sale)).thenReturn(sale);

        service.register(sale);

        assertThat(shampoo.getStock()).isEqualTo(8);
        verify(productDao).update(shampoo);
        verify(saleDao).create(sale);
    }

    @Test
    @DisplayName("register fails and deducts nothing when one line exceeds the stock")
    void registerRejectsInsufficientStock() throws Exception {
        Sale sale = new Sale(customer);
        sale.addLine(shampoo, 3);
        sale.addLine(mask, 5);

        assertThatThrownBy(() -> service.register(sale))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Mascarilla");

        assertThat(shampoo.getStock()).isEqualTo(10);
        assertThat(mask.getStock()).isEqualTo(4);
        verifyNoInteractions(productDao);
        verify(saleDao, never()).create(any(Sale.class));
    }

    @Test
    @DisplayName("register rejects a sale without lines")
    void registerRejectsEmptySale() {
        assertThatThrownBy(() -> service.register(new Sale(customer)))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("al menos una línea");

        verifyNoInteractions(saleDao, productDao);
    }

    @Test
    @DisplayName("register rejects a missing sale")
    void registerRejectsNullSale() {
        assertThatThrownBy(() -> service.register(null)).isInstanceOf(ValidationException.class);

        verifyNoInteractions(saleDao, productDao);
    }

    @Test
    @DisplayName("register rejects a sale that was already registered")
    void registerRejectsRegisteredSale() throws Exception {
        Sale sale = new Sale(customer);
        sale.addLine(shampoo, 1);
        sale.setId(7);

        assertThatThrownBy(() -> service.register(sale)).isInstanceOf(BusinessRuleException.class);

        assertThat(shampoo.getStock()).isEqualTo(10);
        verifyNoInteractions(saleDao, productDao);
    }

    @Test
    @DisplayName("register gives the stock back when the sale cannot be stored")
    void registerRestoresStockWhenStoringFails() throws Exception {
        Sale sale = new Sale(customer);
        sale.addLine(shampoo, 3);
        sale.addLine(mask, 2);
        when(saleDao.create(sale)).thenThrow(new DataAccessException("disco lleno"));

        assertThatThrownBy(() -> service.register(sale))
            .isInstanceOf(DataAccessException.class)
            .hasMessage("disco lleno");

        assertThat(shampoo.getStock()).isEqualTo(10);
        assertThat(mask.getStock()).isEqualTo(4);
    }

    @Test
    @DisplayName("register gives the stock back when a product cannot be stored")
    void registerRestoresStockWhenProductUpdateFails() throws Exception {
        Sale sale = new Sale(customer);
        sale.addLine(shampoo, 3);
        sale.addLine(mask, 2);
        lenient().doThrow(new DataAccessException("sin permiso")).when(productDao).update(mask);

        assertThatThrownBy(() -> service.register(sale)).isInstanceOf(DataAccessException.class);

        assertThat(shampoo.getStock()).isEqualTo(10);
        assertThat(mask.getStock()).isEqualTo(4);
        verify(saleDao, never()).create(any(Sale.class));
    }

    @Test
    @DisplayName("listByCustomer returns the purchases of the customer")
    void listByCustomerReturnsPurchases() throws Exception {
        Sale sale = new Sale(customer);
        when(saleDao.findByCustomer(1)).thenReturn(List.of(sale));

        assertThat(service.listByCustomer(1)).containsExactly(sale);
    }

    @Test
    @DisplayName("listByCustomer rejects an invalid customer id")
    void listByCustomerRejectsInvalidId() {
        assertThatThrownBy(() -> service.listByCustomer(0)).isInstanceOf(ValidationException.class);

        verifyNoInteractions(saleDao);
    }
}
