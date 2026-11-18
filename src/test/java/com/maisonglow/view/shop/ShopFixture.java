package com.maisonglow.view.shop;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.model.Product;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.ProductData;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Real services with an administrator, two customers (Ana and Laura) and two published products, for the tests of the cart and
 * of "Mis compras". The clock is moved by the test: an order expires 30 minutes after it was placed.
 */
public final class ShopFixture {

    public static final char[] PASSWORD = "clave-segura-123".toCharArray();

    /** A clock that the test moves. */
    public static final class Now extends Clock {
        public Instant now = Instant.now();

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    public final Now clock = new Now();
    public final AppContext context;
    /** 48.000 each, 10 units, published. */
    public final Product serum;
    /** 18.000 each, 2 units, published. */
    public final Product esmalte;

    /**
     * Builds the fixture over a folder; it leaves Ana signed in.
     *
     * @param data an empty folder
     * @throws Exception if something cannot be prepared
     */
    public ShopFixture(Path data) throws Exception {
        AppContext.seed(data);
        context = new AppContext(data, clock, AiConfig.simulated());
        context.auth().createFirstAdmin("admin", PASSWORD);
        context.auth().login("admin", PASSWORD);
        context.auth().registerCustomer(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"), "ana", PASSWORD);
        context.auth().registerCustomer(new ContactData("1002", "Laura Pérez", "3002223344", "laura@correo.com"), "laura", PASSWORD);
        serum = context.products().register(new ProductData("Sérum Vitamina C", 1, 48000, 2), 10);
        esmalte = context.products().register(new ProductData("Esmalte Rosa Nude", 3, 18000, 1), 2);
        context.catalog().setPublished(serum.getId(), true);
        context.catalog().setPublished(esmalte.getId(), true);
        asCustomer("ana");
    }

    /** Signs the administrator in. */
    public void asAdmin() throws Exception {
        context.auth().login("admin", PASSWORD);
    }

    /**
     * Signs a customer in.
     *
     * @param username {@code ana} or {@code laura}
     * @throws Exception if the sign-in fails
     */
    public void asCustomer(String username) throws Exception {
        context.auth().login(username, PASSWORD);
    }

    /**
     * Moves the clock forward.
     *
     * @param minutes how many minutes
     */
    public void advance(long minutes) {
        clock.now = clock.now.plusSeconds(minutes * 60);
    }
}
