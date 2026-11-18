package com.maisonglow.view.shop;

import com.maisonglow.model.Commerce.CatalogProduct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Shop rules: availability and filter")
class ShopRulesTest {

    private static CatalogProduct product(String name, String category, int available) {
        return new CatalogProduct(1, name, category, BigDecimal.TEN, available, true, false);
    }

    @Test
    @DisplayName("the shop says available, few units or sold out, and never the figure itself")
    void availabilityWords() {
        assertThat(ProductAvailability.of(10)).isEqualTo(ProductAvailability.AVAILABLE);
        assertThat(ProductAvailability.of(ProductAvailability.FEW_UNITS + 1)).isEqualTo(ProductAvailability.AVAILABLE);
        assertThat(ProductAvailability.of(ProductAvailability.FEW_UNITS)).isEqualTo(ProductAvailability.FEW);
        assertThat(ProductAvailability.of(1)).isEqualTo(ProductAvailability.FEW);
        assertThat(ProductAvailability.of(0)).isEqualTo(ProductAvailability.SOLD_OUT);
        assertThat(ProductAvailability.of(-2)).isEqualTo(ProductAvailability.SOLD_OUT);
        for (ProductAvailability each : ProductAvailability.values()) {
            assertThat(each.label()).doesNotContainPattern("\\d");
        }
        assertThat(ProductAvailability.SOLD_OUT.canBeOrdered()).isFalse();
        assertThat(ProductAvailability.FEW.canBeOrdered()).isTrue();
    }

    @Test
    @DisplayName("the filter takes a category and a text in the name, ignoring case and accents")
    void filterMatches() {
        CatalogProduct champu = product("Champú Nutritivo", "Cabello", 5);

        assertThat(ShopFilter.all().matches(champu)).isTrue();
        assertThat(ShopFilter.all().withText("CHAMPU").matches(champu)).isTrue();
        assertThat(ShopFilter.all().withText("esmalte").matches(champu)).isFalse();
        assertThat(ShopFilter.all().withCategory("Cabello").matches(champu)).isTrue();
        assertThat(ShopFilter.all().withCategory("Uñas").matches(champu)).isFalse();
        assertThat(ShopFilter.all().withCategory("Cabello").withText("nutri").matches(champu)).isTrue();
        assertThat(ShopFilter.all().withText(null).text()).isEmpty();
        assertThat(ShopFilter.all().withText("  ").matches(champu)).isTrue();
    }
}
