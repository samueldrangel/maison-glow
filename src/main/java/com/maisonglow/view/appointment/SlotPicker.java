package com.maisonglow.view.appointment;

import com.maisonglow.util.DateFormats;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The hours a customer can choose for an appointment, as buttons: only the ones the service said are free, one of them can be
 * chosen, and when there is none (or nothing was asked yet) a sentence says why. It only draws and remembers the choice; which
 * hours are free is decided by the appointment service. Used to book and to reschedule.
 */
public final class SlotPicker extends VBox {

    /** First hour offered, every half hour up to {@link #LAST}. */
    public static final LocalTime FIRST = LocalTime.of(7, 0);
    /** Last hour offered. */
    public static final LocalTime LAST = LocalTime.of(19, 30);

    private final FlowPane slots = new FlowPane(8, 8);
    private final Label hint = new Label();
    private final ToggleGroup group = new ToggleGroup();
    private Runnable onChosen = () -> { };

    /** Creates an empty picker. */
    public SlotPicker() {
        super(8);
        hint.getStyleClass().add("form-hint");
        hint.setWrapText(true);
        getChildren().addAll(hint, slots);
        group.selectedToggleProperty().addListener((observable, before, now) -> onChosen.run());
        showHint("");
    }

    /** @return every hour that is offered to be asked about: every half hour from {@link #FIRST} to {@link #LAST} */
    public static List<LocalTime> candidates() {
        List<LocalTime> hours = new ArrayList<>();
        for (LocalTime hour = FIRST; !hour.isAfter(LAST); hour = hour.plusMinutes(30)) {
            hours.add(hour);
        }
        return hours;
    }

    /**
     * Sets what happens when the person chooses an hour.
     *
     * @param action called after the choice
     */
    public void onChosen(Runnable action) {
        this.onChosen = Objects.requireNonNull(action, "action");
    }

    /**
     * Shows a sentence and no hours, for example to say what to choose first.
     *
     * @param text the sentence; empty hides it
     */
    public void showHint(String text) {
        slots.getChildren().clear();
        hint.setText(text);
        hint.setVisible(!text.isEmpty());
        hint.setManaged(!text.isEmpty());
    }

    /**
     * Shows the free hours; with none it says there are none.
     *
     * @param free the hours the service said are free
     */
    public void showSlots(List<LocalTime> free) {
        if (free.isEmpty()) {
            showHint("No hay horarios disponibles ese día. Prueba con otra fecha o con otro profesional.");
            return;
        }
        hint.setVisible(false);
        hint.setManaged(false);
        slots.getChildren().setAll(free.stream().map(time -> {
            ToggleButton button = new ToggleButton(DateFormats.time(time));
            button.getStyleClass().add("filter-chip");
            button.setUserData(time);
            button.setToggleGroup(group);
            return button;
        }).toList());
    }

    /** @return the value that changes when an hour is chosen or cleared */
    public javafx.beans.value.ObservableValue<?> selection() {
        return group.selectedToggleProperty();
    }

    /** @return the hour chosen, empty if none */
    public Optional<LocalTime> selected() {
        return group.getSelectedToggle() == null ? Optional.empty() : Optional.of((LocalTime) group.getSelectedToggle().getUserData());
    }
}
