package com.maisonglow.view.sales;

import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.ProductCategory;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Treatment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Cart tests")
class CartTest {

    private Cart cart;
    private Product shampoo;
    private Treatment cut;

    @BeforeEach
    void setUp() {
        cart = new Cart();
        shampoo = new Product(1, "Champú Nutritivo", new ProductCategory(1, "Cabello"), new BigDecimal("28000"), 10, 5);
        cut = new Treatment(1, "Corte", "Corte de cabello", new BigDecimal("45000"), 60);
    }

    @Test
    @DisplayName("is empty at first and has a zero total")
    void startsEmpty() {
        assertThat(cart.lines().isEmpty()).isTrue();
        assertThat(cart.total().get()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("adding the same item twice raises its quantity instead of repeating the line")
    void mergesRepeatedItems() {
        cart.add(shampoo);
        cart.add(shampoo);

        assertThat(cart.lines()).hasSize(1);
        assertThat(cart.lines().get(0).getQuantity()).isEqualTo(2);
        assertThat(cart.total().get()).isEqualByComparingTo("56000");
    }

    @Test
    @DisplayName("the total adds products and treatments and follows every change")
    void totalFollowsChanges() {
        cart.add(shampoo);
        cart.add(cut);
        assertThat(cart.total().get()).isEqualByComparingTo("73000");

        cart.increase(cart.lines().get(0));
        assertThat(cart.total().get()).isEqualByComparingTo("101000");

        cart.remove(cart.lines().get(1));
        assertThat(cart.total().get()).isEqualByComparingTo("56000");
    }

    @Test
    @DisplayName("a line never goes below one unit when it is decreased")
    void decreaseStopsAtOne() {
        cart.add(shampoo);
        cart.increase(cart.lines().get(0));

        cart.decrease(cart.lines().get(0));
        cart.decrease(cart.lines().get(0));

        assertThat(cart.lines().get(0).getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("builds a sale with copies of the lines, so emptying the cart does not touch it")
    void buildsASale() {
        cart.add(shampoo);
        cart.add(shampoo);
        Customer customer = new Customer(1, "1001", "Ana Pérez", "3001112233", "ana@correo.com");

        Sale sale = cart.toSale(customer);
        cart.clear();

        assertThat(cart.lines().isEmpty()).isTrue();
        assertThat(sale.getCustomer()).isEqualTo(customer);
        assertThat(sale.getLines()).extracting(SaleLine::getQuantity).containsExactly(2);
        assertThat(sale.getTotal()).isEqualByComparingTo("56000");
    }
}
