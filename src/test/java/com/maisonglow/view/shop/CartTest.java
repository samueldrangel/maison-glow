package com.maisonglow.view.shop;

import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Commerce.CatalogProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cart rules")
class CartTest {

    private Cart cart;

    @BeforeEach
    void create() {
        cart = new Cart();
    }

    private static CatalogProduct product(int id, String name, String price, int available) {
        return new CatalogProduct(id, name, "Piel", new BigDecimal(price), available, true, false);
    }

    @Test
    @DisplayName("adding adds up the units of the same product and totals the cart with the prices the shop showed")
    void addsAndTotals() throws Exception {
        cart.add(product(1, "Sérum", "48000", 10), 2);
        cart.add(product(1, "Sérum", "48000", 10), 1);
        cart.add(product(2, "Esmalte", "18000", 5), 1);

        assertThat(cart.quantityOf(1)).isEqualTo(3);
        assertThat(cart.itemCount()).isEqualTo(4);
        assertThat(cart.total()).isEqualByComparingTo("162000");
        assertThat(cart.lines()).extracting(Cart.Line::name).containsExactly("Esmalte", "Sérum");
        assertThat(cart.quantities()).isEqualTo(Map.of(1, 3, 2, 1));
    }

    @Test
    @DisplayName("it never holds more than is available, nor a sold-out product, nor less than one unit")
    void limits() throws Exception {
        cart.add(product(1, "Sérum", "48000", 3), 2);

        assertThatThrownBy(() -> cart.add(product(1, "Sérum", "48000", 3), 2)).isInstanceOf(ValidationException.class)
            .hasMessageContaining("Solo hay 3 unidades");
        assertThatThrownBy(() -> cart.add(product(2, "Esmalte", "18000", 0), 1)).isInstanceOf(ValidationException.class)
            .hasMessageContaining("agotado");
        assertThatThrownBy(() -> cart.add(product(3, "Otro", "1000", 5), 0)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> cart.setQuantity(1, 4)).isInstanceOf(ValidationException.class);
        assertThat(cart.quantityOf(1)).isEqualTo(2);
    }

    @Test
    @DisplayName("setting the quantity changes it, and zero takes the product out")
    void setsQuantity() throws Exception {
        cart.add(product(1, "Sérum", "48000", 5), 1);

        cart.setQuantity(1, 4);
        assertThat(cart.quantityOf(1)).isEqualTo(4);
        cart.setQuantity(1, 0);

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.total()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("the request key stays the same while the cart does not change and is new after any change")
    void requestKey() throws Exception {
        cart.add(product(1, "Sérum", "48000", 5), 1);

        String key = cart.requestKey();
        assertThat(cart.requestKey()).isEqualTo(key).matches("[a-zA-Z0-9_-]{8,100}");

        cart.add(product(1, "Sérum", "48000", 5), 1);
        assertThat(cart.requestKey()).isNotEqualTo(key);
        String second = cart.requestKey();
        cart.remove(1);
        assertThat(cart.requestKey()).isNotEqualTo(second);
    }

    @Test
    @DisplayName("reconciling tells a changed price, fewer units and a withdrawn product, and updates the cart")
    void reconciles() throws Exception {
        cart.add(product(1, "Sérum", "48000", 10), 4);
        cart.add(product(2, "Esmalte", "18000", 5), 2);
        cart.add(product(3, "Labial", "30000", 5), 1);

        List<String> changes = cart.reconcile(Map.of(
            1, product(1, "Sérum", "50000", 10),
            2, product(2, "Esmalte", "18000", 1)));

        assertThat(changes).hasSize(3);
        assertThat(changes).anyMatch(text -> text.contains("Sérum") && text.contains("48.000") && text.contains("50.000"));
        assertThat(changes).anyMatch(text -> text.contains("Esmalte") && text.contains("solo quedan 1"));
        assertThat(changes).anyMatch(text -> text.contains("Labial") && text.contains("se quitó"));
        assertThat(cart.quantityOf(1)).isEqualTo(4);
        assertThat(cart.quantityOf(2)).isEqualTo(1);
        assertThat(cart.quantityOf(3)).isZero();
        assertThat(cart.total()).isEqualByComparingTo("218000");
    }

    @Test
    @DisplayName("reconciling with nothing changed says nothing and keeps the same key")
    void reconcileWithoutChanges() throws Exception {
        cart.add(product(1, "Sérum", "48000", 10), 2);
        String key = cart.requestKey();

        assertThat(cart.reconcile(Map.of(1, product(1, "Sérum", "48000", 10)))).isEmpty();

        assertThat(cart.requestKey()).isEqualTo(key);
    }

    @Test
    @DisplayName("clearing empties the cart and the next key is new")
    void clears() throws Exception {
        cart.add(product(1, "Sérum", "48000", 10), 2);
        String key = cart.requestKey();

        cart.clear();

        assertThat(cart.isEmpty()).isTrue();
        assertThat(cart.requestKey()).isNotEqualTo(key);
    }
}
