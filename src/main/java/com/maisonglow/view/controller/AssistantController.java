package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.view.ViewLifecycle;
import com.maisonglow.ai.AssistantReply;
import com.maisonglow.ai.PendingAction;
import com.maisonglow.ai.SmartAssistant;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.service.CustomerService;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.util.DateFormats;
import com.maisonglow.view.assistant.PendingActionRows;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;

/**
 * Controller of {@code AssistantView.fxml}: the chat with the assistant (mockup M9). Whatever the user writes goes
 * to the assistant; when the assistant wants to do something that changes data it does not do it: the reply carries
 * a pending action and the chat shows its details with "Confirmar" and "Cancelar", and nothing happens until the
 * user chooses. The assistant can take a while to answer, so it is asked in the background and the chat keeps
 * answering to the window.
 */
public final class AssistantController implements ViewLifecycle {

    private static final String WELCOME = "Hola, soy el asistente de Maison Glow. Pregúntame por el stock, las citas o los "
        + "clientes. Si me pides agendar una cita, te mostraré los datos y esperaré tu confirmación.";

    private final SmartAssistant assistant;
    private final boolean simulated;
    private final CustomerService customers;
    private final ProfessionalService professionals;
    private final TreatmentService treatments;
    private final Clock clock;
    private final PendingActionRows rows;
    private Executor executor = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "asistente");
        thread.setDaemon(true);
        return thread;
    });
    private boolean waiting;

    @FXML
    private HBox simulatedBanner;
    @FXML
    private ScrollPane chatScroll;
    @FXML
    private VBox messagesBox;
    private boolean disposed;
    private final java.util.List<PendingDecision> unresolved = new java.util.ArrayList<>();

    /** An action waiting for Confirmar or Cancelar, with the buttons that decide it. */
    private record PendingDecision(PendingAction action, HBox buttons) { }
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button lowStockShortcut;
    @FXML
    private Button todayShortcut;
    @FXML
    private Button customerShortcut;
    @FXML
    private Button helpShortcut;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public AssistantController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.assistant = context.ai().assistant();
        this.simulated = context.ai().isSimulated();
        this.customers = context.customers();
        this.professionals = context.professionals();
        this.treatments = context.treatments();
        this.clock = context.clock();
        this.rows = new PendingActionRows(
            id -> nameOf(() -> customers.findById(id).getName()),
            id -> nameOf(() -> professionals.findById(id).getName()),
            id -> nameOf(() -> treatments.findById(id).getName()));
    }

    /**
     * Replaces where the assistant is asked, which the tests use to answer on the spot.
     *
     * @param newExecutor runs the questions to the assistant
     */
    public void useExecutor(Executor newExecutor) {
        this.executor = Objects.requireNonNull(newExecutor, "newExecutor");
    }

    @FXML
    private void initialize() {
        messagesBox.getChildren().clear();
        simulatedBanner.setVisible(simulated);
        simulatedBanner.setManaged(simulated);
        messagesBox.heightProperty().addListener((observable, before, height) -> chatScroll.setVvalue(1.0));
        sendButton.setOnAction(event -> ask(messageField.getText()));
        messageField.setOnAction(event -> ask(messageField.getText()));
        lowStockShortcut.setOnAction(event -> ask("stock bajo"));
        todayShortcut.setOnAction(event -> ask("citas " + LocalDate.now(clock)));
        helpShortcut.setOnAction(event -> ask("ayuda"));
        customerShortcut.setOnAction(event -> {
            messageField.setText("buscar cliente ");
            messageField.requestFocus();
            messageField.positionCaret(messageField.getText().length());
        });
        addAssistant(WELCOME);
    }

    private interface Lookup {
        String get() throws BusinessRuleException, DataAccessException;
    }

    private static String nameOf(Lookup lookup) {
        try {
            return lookup.get();
        } catch (BusinessRuleException | DataAccessException e) {
            return null;
        }
    }

    /** Sends what the user wrote to the assistant and shows its answer when it comes. */
    private void ask(String text) {
        String message = text == null ? "" : text.trim();
        if (message.isEmpty() || waiting) {
            return;
        }
        addMessage("bubble-user", message, Pos.CENTER_RIGHT);
        messageField.clear();
        Node typing = bubble("bubble-assistant", "Escribiendo…", Pos.CENTER_LEFT);
        messagesBox.getChildren().add(typing);
        answer(typing, () -> assistant.reply(message));
    }

    /** Runs a call to the assistant in the background; the answer takes the place of the "typing" mark. */
    private void answer(Node typing, java.util.function.Supplier<AssistantReply> call) {
        setWaiting(true);
        CompletableFuture.supplyAsync(call, executor).whenComplete((reply, error) -> Platform.runLater(() -> {
            if (disposed) {
                return;
            }
            messagesBox.getChildren().remove(typing);
            if (error != null) {
                addAssistant("No pude responder en este momento. Inténtalo de nuevo.");
            } else {
                addAssistant(reply.text());
                reply.pendingConfirmation().ifPresent(this::addConfirmation);
            }
            setWaiting(false);
        }));
    }

    private void setWaiting(boolean value) {
        waiting = value;
        sendButton.setDisable(value);
        messageField.setDisable(value);
    }

    private void addAssistant(String text) {
        addMessage("bubble-assistant", text, Pos.CENTER_LEFT);
    }

    private void addMessage(String style, String text, Pos side) {
        Label time = new Label(DateFormats.time(LocalTime.now(clock)));
        time.getStyleClass().add("bubble-time");
        VBox line = new VBox(2, bubble(style, text, side), time);
        line.setAlignment(side);
        messagesBox.getChildren().add(line);
    }

    private static Node bubble(String style, String text, Pos side) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(style);
        VBox box = new VBox(label);
        box.setAlignment(side);
        return box;
    }

    /** The card that asks to confirm an action: what it is, its data, and two buttons. Nothing is done before one is pressed. */
    private void addConfirmation(PendingAction action) {
        Label title = new Label("Confirmar acción: " + PendingActionRows.titleOf(action));
        title.getStyleClass().add("confirm-title");
        Label note = new Label("Revisa los detalles antes de confirmar. No se hará ningún cambio hasta que pulses «Confirmar».");
        note.getStyleClass().add("form-hint");
        note.setWrapText(true);
        VBox card = new VBox(8, title, note);
        card.getStyleClass().add("confirm-card");
        for (PendingActionRows.Row row : rows.rowsOf(action)) {
            Label label = new Label(row.label());
            label.getStyleClass().add("stat-caption");
            Label value = new Label(row.value());
            value.getStyleClass().add("stat-value");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            HBox line = new HBox(label, gap, value);
            line.getStyleClass().add("confirm-row");
            card.getChildren().add(line);
        }
        Button confirm = new Button("Confirmar");
        confirm.getStyleClass().addAll("button-gold", "button-compact");
        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().addAll("button-secondary", "button-compact");
        HBox buttons = new HBox(10, confirm, cancel);
        HBox.setHgrow(confirm, Priority.ALWAYS);
        HBox.setHgrow(cancel, Priority.ALWAYS);
        confirm.setMaxWidth(Double.MAX_VALUE);
        cancel.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(buttons);
        confirm.setOnAction(event -> decide(buttons, () -> assistant.confirm(action)));
        cancel.setOnAction(event -> decide(buttons, () -> assistant.reject(action)));
        unresolved.add(new PendingDecision(action, buttons));
        messagesBox.getChildren().add(card);
    }

    /** Answers a confirmation once: the buttons go off so the same action cannot be decided twice. */
    private void decide(HBox buttons, java.util.function.Supplier<AssistantReply> call) {
        if (disposed) {
            return;
        }
        unresolved.removeIf(pending -> pending.buttons() == buttons);
        buttons.getChildren().forEach(button -> button.setDisable(true));
        Node typing = bubble("bubble-assistant", "Escribiendo…", Pos.CENTER_LEFT);
        messagesBox.getChildren().add(typing);
        answer(typing, call);
    }

    /** Closing the session: whatever waited for a confirmation is rejected, its buttons go off and a late answer is ignored. */
    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (PendingDecision pending : unresolved) {
            pending.buttons().getChildren().forEach(button -> button.setDisable(true));
            try {
                assistant.reject(pending.action());
            } catch (RuntimeException ignored) {
                // The pending action is already unusable: the buttons are off and the screen is discarded.
            }
        }
        unresolved.clear();
    }
}
