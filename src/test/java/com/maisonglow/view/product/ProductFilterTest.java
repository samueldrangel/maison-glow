package com.maisonglow.view.product;

import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductFilter tests")
class ProductFilterTest {

    private static final ProductCategory HAIR = new ProductCategory(1, "Cabello");
    private static final ProductCategory NAILS = new ProductCategory(3, "Uñas");

    private final Product shampoo = new Product(1, "Champú Nutritivo", HAIR, 28000, 5, 10);
    private final Product polish = new Product(2, "Esmalte Rosa Nude", NAILS, 18000, 0, 8);
    private final Product oil = new Product(3, "Aceite de Cutículas", NAILS, 16000, 12, 6);
    private final List<Product> products = List.of(shampoo, polish, oil);

    private List<Product> shown(ProductFilter filter) {
        return products.stream().filter(filter::matches).toList();
    }

    @Test
    @DisplayName("lets everything through when nothing is chosen")
    void allPasses() {
        assertThat(shown(ProductFilter.all())).containsExactly(shampoo, polish, oil);
    }

    @Test
    @DisplayName("searches by name without caring for accents or capitals")
    void searchesByName() {
        assertThat(shown(ProductFilter.all().withText("champu"))).containsExactly(shampoo);
        assertThat(shown(ProductFilter.all().withText("ESMALTE"))).containsExactly(polish);
        assertThat(shown(ProductFilter.all().withText("xyz"))).isEmpty();
    }

    @Test
    @DisplayName("filters by category")
    void filtersByCategory() {
        assertThat(shown(ProductFilter.all().withCategory(3))).containsExactly(polish, oil);
        assertThat(shown(ProductFilter.all().withCategory(1))).containsExactly(shampoo);
    }

    @Test
    @DisplayName("shows only what has to be restocked")
    void onlyRestock() {
        assertThat(shown(ProductFilter.all().withOnlyRestock(true))).containsExactly(shampoo, polish);
    }

    @Test
    @DisplayName("combines the three conditions")
    void combines() {
        ProductFilter filter = ProductFilter.all().withCategory(3).withOnlyRestock(true).withText("rosa");

        assertThat(shown(filter)).containsExactly(polish);
        assertThat(shown(filter.withText("aceite"))).isEmpty();
    }

    @Test
    @DisplayName("each change gives a new filter and a missing text means no search")
    void isAValue() {
        ProductFilter base = ProductFilter.all();

        assertThat(base.withText("x")).isNotEqualTo(base);
        assertThat(base.withText(null).text()).isEmpty();
        assertThat(base.text()).isEmpty();
    }
}
