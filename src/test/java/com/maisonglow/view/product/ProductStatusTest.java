package com.maisonglow.view.product;

import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductStatus tests")
class ProductStatusTest {

    private static Product product(int stock, int minimum, boolean active) {
        Product product = new Product(1, "Shampoo", new ProductCategory(1, "Cabello"), 28000, stock, minimum);
        product.setActive(active);
        return product;
    }

    @Test
    @DisplayName("is available while the stock is above the minimum")
    void available() {
        assertThat(ProductStatus.of(product(24, 8, true))).isEqualTo(ProductStatus.AVAILABLE);
    }

    @Test
    @DisplayName("has few units when the stock is at or below the minimum")
    void lowStock() {
        assertThat(ProductStatus.of(product(5, 10, true))).isEqualTo(ProductStatus.LOW_STOCK);
        assertThat(ProductStatus.of(product(10, 10, true))).as("at the minimum already counts").isEqualTo(ProductStatus.LOW_STOCK);
    }

    @Test
    @DisplayName("has no stock when there are no units")
    void outOfStock() {
        assertThat(ProductStatus.of(product(0, 8, true))).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("an inactive product is inactive whatever its stock")
    void inactiveWins() {
        assertThat(ProductStatus.of(product(0, 8, false))).isEqualTo(ProductStatus.INACTIVE);
        assertThat(ProductStatus.of(product(50, 8, false))).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    @DisplayName("only the out of stock and low stock ones need restocking")
    void needsRestock() {
        assertThat(ProductStatus.OUT_OF_STOCK.needsRestock()).isTrue();
        assertThat(ProductStatus.LOW_STOCK.needsRestock()).isTrue();
        assertThat(ProductStatus.AVAILABLE.needsRestock()).isFalse();
        assertThat(ProductStatus.INACTIVE.needsRestock()).isFalse();
    }

    @Test
    @DisplayName("each status has a label and a chip style of the stylesheet")
    void eachStatusHasItsChip() {
        for (ProductStatus status : ProductStatus.values()) {
            assertThat(status.label()).isNotBlank();
            assertThat(status.chipStyle()).startsWith("chip-");
        }
    }
}
