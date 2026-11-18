package com.maisonglow.view;

import javafx.application.Platform;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Runs something slow (a call to a service that hashes a password, to the AI or to the disk) off the JavaFX thread and
 * brings the result back on it. The screen decides, when the answer arrives, if it still wants it: after the session
 * closed or the screen was discarded the answer is dropped, so it never shows up in the next session.
 *
 * <pre>{@code
 * BackgroundTask.run(() -> service.slowCall(), result -> show(result), error -> alerts.showError(error), () -> !disposed);
 * }</pre>
 */
public final class BackgroundTask {

    private BackgroundTask() {
    }

    /**
     * Runs {@code work} in the background and then {@code onSuccess} or {@code onError} on the JavaFX thread.
     *
     * @param work        the slow call; it must not touch the screen
     * @param onSuccess   receives the result, on the JavaFX thread
     * @param onError     receives what {@code work} threw, on the JavaFX thread
     * @param stillWanted asked when the answer arrives; if it says false nothing is called
     * @param <T>         the type of the result
     */
    public static <T> void run(Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError, BooleanSupplier stillWanted) {
        run(ForkJoinPool.commonPool(), work, onSuccess, onError, stillWanted);
    }

    /**
     * Same as {@link #run(Callable, Consumer, Consumer, BooleanSupplier)} with its own executor, which the tests use
     * to hold an answer back.
     *
     * @param executor    where {@code work} runs
     * @param work        the slow call; it must not touch the screen
     * @param onSuccess   receives the result, on the JavaFX thread
     * @param onError     receives what {@code work} threw, on the JavaFX thread
     * @param stillWanted asked when the answer arrives; if it says false nothing is called
     * @param <T>         the type of the result
     */
    public static <T> void run(Executor executor, Callable<T> work, Consumer<T> onSuccess, Consumer<Exception> onError,
                               BooleanSupplier stillWanted) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(work, "work");
        Objects.requireNonNull(onSuccess, "onSuccess");
        Objects.requireNonNull(onError, "onError");
        Objects.requireNonNull(stillWanted, "stillWanted");
        CompletableFuture.supplyAsync(() -> {
            try {
                return work.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor).whenComplete((result, error) -> Platform.runLater(() -> {
            if (!stillWanted.getAsBoolean()) {
                return;
            }
            if (error == null) {
                onSuccess.accept(result);
            } else {
                Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
                onError.accept(cause instanceof Exception exception ? exception : new IllegalStateException(cause));
            }
        }));
    }
}
