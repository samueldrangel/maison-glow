package com.maisonglow.service;

import com.maisonglow.dao.CategoryDao;
import com.maisonglow.dao.ProductDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService tests")
class ProductServiceTest {

    private static final ProductCategory HAIR = new ProductCategory(1, "Cabello");

    @Mock
    private ProductDao productDao;

    @Mock
    private CategoryDao categoryDao;

    private ProductService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ProductService(productDao, categoryDao);
        lenient().when(categoryDao.findById(1)).thenReturn(Optional.of(HAIR));
        lenient().when(categoryDao.findById(9)).thenReturn(Optional.empty());
    }

    private Product shampoo() {
        return new Product(1, "Shampoo", HAIR, 28000, 10, 3);
    }

    @Test
    @DisplayName("register stores a valid product")
    void registerValidProduct() throws Exception {
        when(productDao.findByName("Shampoo")).thenReturn(Optional.empty());
        when(productDao.create(any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product product = service.register(new ProductData(" Shampoo ", 1, 28000, 3), 10);

        assertThat(product.getName()).isEqualTo("Shampoo");
        assertThat(product.getCategory()).isEqualTo(HAIR);
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("register rejects a repeated name and an unknown category")
    void registerRejectsDuplicatesAndUnknownCategory() throws Exception {
        when(productDao.findByName("Shampoo")).thenReturn(Optional.of(shampoo()));
        when(productDao.findByName("Acondicionador")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(new ProductData("Shampoo", 1, 1000, 0), 1))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Ya existe");
        assertThatThrownBy(() -> service.register(new ProductData("Acondicionador", 9, 1000, 0), 1))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("categoría");
        verify(productDao, never()).create(any());
    }

    @Test
    @DisplayName("register rejects invalid price and negative stock")
    void registerRejectsInvalidNumbers() {
        assertThatThrownBy(() -> service.register(new ProductData("Gel", 1, 0, 0), 1))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new ProductData("Gel", 1, 100, 0), -1))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new ProductData("Gel", 1, 100, -2), 1))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new ProductData(" ", 1, 100, 0), 1))
            .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("update changes data and allows keeping the same name")
    void updateProduct() throws Exception {
        Product product = shampoo();
        when(productDao.findById(1)).thenReturn(Optional.of(product));
        when(productDao.findByName("Shampoo")).thenReturn(Optional.of(product));

        service.update(1, new ProductData("Shampoo", 1, 30000, 5));

        assertThat(product.getPrice()).isEqualTo(30000);
        assertThat(product.getMinimumStock()).isEqualTo(5);
        verify(productDao).update(product);
    }

    @Test
    @DisplayName("update rejects a name used by another product")
    void updateRejectsTakenName() throws Exception {
        Product other = new Product(2, "Gel", HAIR, 15000, 4, 1);
        when(productDao.findById(1)).thenReturn(Optional.of(shampoo()));
        when(productDao.findByName("Gel")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(1, new ProductData("Gel", 1, 30000, 5)))
            .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("adjustStock adds and removes units")
    void adjustStock() throws Exception {
        Product product = shampoo();
        when(productDao.findById(1)).thenReturn(Optional.of(product));

        service.adjustStock(1, 5);
        service.adjustStock(1, -12);

        assertThat(product.getStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("adjustStock rejects zero and removing more than available")
    void adjustStockErrors() throws Exception {
        Product product = shampoo();
        when(productDao.findById(1)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.adjustStock(1, 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.adjustStock(1, -11)).isInstanceOf(BusinessRuleException.class);
        assertThat(product.getStock()).isEqualTo(10);
        verify(productDao, never()).update(any());
    }

    @Test
    @DisplayName("listLowStock returns only active products at or below the minimum")
    void listLowStock() throws Exception {
        Product low = new Product(2, "Gel", HAIR, 15000, 1, 2);
        Product inactiveLow = new Product(3, "Laca", HAIR, 18000, 0, 2);
        inactiveLow.deactivate();
        when(productDao.findAll()).thenReturn(List.of(shampoo(), low, inactiveLow));

        assertThat(service.listLowStock()).containsExactly(low);
        assertThat(service.listActive()).containsExactly(shampoo(), low);
        assertThat(service.listAll()).hasSize(3);
    }

    @Test
    @DisplayName("deactivate and activate are stored")
    void deactivateAndActivate() throws Exception {
        Product product = shampoo();
        when(productDao.findById(1)).thenReturn(Optional.of(product));

        service.deactivate(1);
        assertThat(product.isActive()).isFalse();

        service.activate(1);
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("findById fails for unknown products and categories are listed")
    void findUnknownAndCategories() throws Exception {
        when(productDao.findById(7)).thenReturn(Optional.empty());
        when(categoryDao.findAll()).thenReturn(List.of(HAIR));

        assertThatThrownBy(() -> service.findById(7)).isInstanceOf(BusinessRuleException.class);
        assertThat(service.listCategories()).containsExactly(HAIR);
    }
}
