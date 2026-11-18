package com.maisonglow.view;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Starts the JavaFX toolkit once and runs code on its thread, for the tests of the views. */
public final class FxTest {

    private static boolean available;

    private FxTest() {
    }

    /** @return false when there is no display, so the test can be skipped */
    public static synchronized boolean start() {
        try {
            Platform.startup(() -> { });
            available = true;
        } catch (IllegalStateException alreadyStarted) {
            available = true;
        } catch (UnsupportedOperationException noDisplay) {
            available = false;
        }
        if(available)Platform.setImplicitExit(false);
        return available;
    }

    public static <T> T onFxThread(Supplier<T> action) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(action.get());
            } catch (RuntimeException | AssertionError e) {
                result.completeExceptionally(e);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }
}
