package com.maisonglow.model;

import com.maisonglow.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Product tests")
class ProductTest {

    private static final ProductCategory HAIR = new ProductCategory(1, "Cabello");

    private Product shampoo() {
        return new Product(1, "Shampoo hidratante", HAIR, 28000, 10, 3);
    }

    @Test
    @DisplayName("a new product is active and exposes its data as Sellable")
    void newProductIsActive() {
        Product product = shampoo();
        Sellable sellable = product;

        assertThat(product.isActive()).isTrue();
        assertThat(product.getCategory()).isEqualTo(HAIR);
        assertThat(sellable.getName()).isEqualTo("Shampoo hidratante");
        assertThat(sellable.getPrice()).isEqualTo(28000);
    }

    @Test
    @DisplayName("deducting exactly the available stock leaves zero")
    void deductExactStock() throws BusinessRuleException {
        Product product = shampoo();

        product.deductStock(10);

        assertThat(product.getStock()).isZero();
    }

    @Test
    @DisplayName("deducting more than the stock fails and keeps the stock")
    void deductTooMuchStock() {
        Product product = shampoo();

        assertThatThrownBy(() -> product.deductStock(11))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("Stock insuficiente");
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("hasStock compares the requested units with the stock")
    void hasStock() {
        assertThat(shampoo().hasStock(10)).isTrue();
        assertThat(shampoo().hasStock(11)).isFalse();
    }

    @Test
    @DisplayName("addStock increases the available units")
    void addStock() throws BusinessRuleException {
        Product product = shampoo();

        product.addStock(5);

        assertThat(product.getStock()).isEqualTo(15);
    }

    @Test
    @DisplayName("deducting zero or a negative quantity fails and keeps the stock")
    void deductNonPositiveQuantity() {
        Product product = shampoo();

        assertThatThrownBy(() -> product.deductStock(-3))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> product.deductStock(0))
            .isInstanceOf(BusinessRuleException.class);
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("adding zero or a negative quantity fails and keeps the stock")
    void addNonPositiveQuantity() {
        Product product = shampoo();

        assertThatThrownBy(() -> product.addStock(-10))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("mayor que cero");
        assertThatThrownBy(() -> product.addStock(0))
            .isInstanceOf(BusinessRuleException.class);
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("isBelowMinimum is true at and under the threshold")
    void belowMinimumThreshold() throws BusinessRuleException {
        Product product = shampoo();
        assertThat(product.isBelowMinimum()).isFalse();

        product.deductStock(7);
        assertThat(product.isBelowMinimum()).isTrue();

        product.deductStock(1);
        assertThat(product.isBelowMinimum()).isTrue();
    }

    @Test
    @DisplayName("deactivate and activate toggle availability")
    void deactivateAndActivate() {
        Product product = shampoo();

        product.deactivate();
        assertThat(product.isActive()).isFalse();

        product.activate();
        assertThat(product.isActive()).isTrue();
    }

    @Test
    @DisplayName("products and categories are equal by id")
    void equalityById() {
        Product renamed = new Product(1, "Shampoo", HAIR, 30000, 0, 0);

        assertThat(shampoo()).isEqualTo(renamed).hasSameHashCodeAs(renamed);
        assertThat(new ProductCategory(1, "Pelo")).isEqualTo(HAIR);
        assertThat(HAIR).hasToString("Cabello");
    }
}
