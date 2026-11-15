package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.SettingsService;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FieldValidator;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Controller of {@code SettingsView.fxml}: edits the business parameters kept by
 * {@link SettingsService}. The service validates every value, so a number out of
 * range is rejected under its own field and never reaches {@code settings.txt}.
 */
public final class SettingsController {

    private final SettingsService settings;
    private final FieldValidator fields = new FieldValidator();
    /** The fields by the setting they edit, in the order they are saved. */
    private final Map<String, TextField> byKey = new LinkedHashMap<>();
    /** What the file held when the screen was loaded; restored if a save fails halfway. */
    private final Map<String, String> loaded = new LinkedHashMap<>();
    /** How each setting is called on screen; the service names them by their key. */
    private final Map<String, String> names = new LinkedHashMap<>();
    private AlertPresenter alerts;

    @FXML
    private TextField businessField;
    @FXML
    private Label businessMessage;
    @FXML
    private TextField depositField;
    @FXML
    private Label depositMessage;
    @FXML
    private TextField noticeField;
    @FXML
    private Label noticeMessage;
    @FXML
    private TextField reminderField;
    @FXML
    private Label reminderMessage;
    @FXML
    private TextField noShowField;
    @FXML
    private Label noShowMessage;
    @FXML
    private TextField windowField;
    @FXML
    private Label windowMessage;
    @FXML
    private TextField topField;
    @FXML
    private Label topMessage;
    @FXML
    private TextField discountField;
    @FXML
    private Label discountMessage;
    @FXML
    private Button saveButton;
    @FXML
    private Button discardButton;

    /**
     * Creates the controller.
     *
     * @param context services of the application
     */
    public SettingsController(AppContext context) {
        this.settings = Objects.requireNonNull(context, "context").settings();
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(saveButton);
        register(SettingsService.BUSINESS_NAME, "Nombre del negocio", businessField, businessMessage);
        register(SettingsService.DEPOSIT_PERCENTAGE, "Anticipo", depositField, depositMessage);
        register(SettingsService.MIN_BOOKING_NOTICE_HOURS, "Anticipación mínima", noticeField, noticeMessage);
        register(SettingsService.REMINDER_HOURS_BEFORE, "Recordatorio", reminderField, reminderMessage);
        register(SettingsService.NO_SHOW_PENALTY_THRESHOLD, "Inasistencias", noShowField, noShowMessage);
        register(SettingsService.LOW_ROTATION_WINDOW_DAYS, "Ventana de ventas", windowField, windowMessage);
        register(SettingsService.LOW_ROTATION_TOP_N, "Cuántos se reportan", topField, topMessage);
        register(SettingsService.LOW_ROTATION_DISCOUNT_PERCENTAGE, "Descuento", discountField, discountMessage);
        saveButton.setOnAction(event -> save());
        discardButton.setOnAction(event -> discard());
        load();
    }

    private void register(String key, String name, TextField field, Label message) {
        byKey.put(key, field);
        names.put(key, name);
        // The message is rewritten to name the setting as the screen does, so that name finds the field to mark.
        fields.field(field, message, quoted(name));
        field.textProperty().addListener((observable, before, now) -> refreshButtons());
    }

    /** Reads the saved values into the fields. */
    private void load() {
        loaded.clear();
        try {
            loaded.put(SettingsService.BUSINESS_NAME, settings.businessName());
            loaded.put(SettingsService.DEPOSIT_PERCENTAGE, number(settings.depositPercentage()));
            loaded.put(SettingsService.MIN_BOOKING_NOTICE_HOURS, String.valueOf(settings.minBookingNoticeHours()));
            loaded.put(SettingsService.REMINDER_HOURS_BEFORE, String.valueOf(settings.reminderHoursBefore()));
            loaded.put(SettingsService.NO_SHOW_PENALTY_THRESHOLD, String.valueOf(settings.noShowPenaltyThreshold()));
            loaded.put(SettingsService.LOW_ROTATION_WINDOW_DAYS, String.valueOf(settings.lowRotationWindowDays()));
            loaded.put(SettingsService.LOW_ROTATION_TOP_N, String.valueOf(settings.lowRotationTopN()));
            loaded.put(SettingsService.LOW_ROTATION_DISCOUNT_PERCENTAGE, number(settings.lowRotationDiscountPercentage()));
        } catch (ValidationException | DataAccessException e) {
            alerts.showError(e);
        }
        fields.clear();
        byKey.forEach((key, field) -> field.setText(loaded.getOrDefault(key, "")));
        refreshButtons();
    }

    private void discard() {
        load();
    }

    private void save() {
        fields.clear();
        Map<String, String> saved = new LinkedHashMap<>();
        for (Map.Entry<String, TextField> entry : byKey.entrySet()) {
            String key = entry.getKey();
            String value = typed(key, entry.getValue());
            if (!loaded.containsKey(key) || value.equals(loaded.get(key))) {
                continue;
            }
            try {
                settings.update(key, value);
                saved.put(key, loaded.get(key));
            } catch (ValidationException | DataAccessException e) {
                restore(saved);
                report(key, e);
                return;
            }
        }
        loaded.clear();
        byKey.forEach((key, field) -> loaded.put(key, typed(key, field)));
        refreshButtons();
        alerts.toast("Configuración guardada");
    }

    /** Marks the field of the setting that was rejected, or opens a dialog for any other failure. */
    private void report(String key, Exception error) {
        if (error instanceof ValidationException) {
            String text = String.valueOf(error.getMessage()).replace(key, quoted(names.get(key)));
            if (fields.show(new ValidationException(text, error))) {
                return;
            }
        }
        alerts.showError(error);
    }

    private static String quoted(String name) {
        return "«" + name + "»";
    }

    /** Puts back the values already written, so a rejected value never leaves a half-saved screen. */
    private void restore(Map<String, String> written) {
        written.forEach((key, previous) -> {
            try {
                settings.update(key, previous);
            } catch (ValidationException | DataAccessException ignored) {
                // The previous value was valid when it was read; nothing more can be done here.
            }
        });
    }

    private void refreshButtons() {
        boolean changed = byKey.entrySet().stream()
            .anyMatch(entry -> !typed(entry.getKey(), entry.getValue()).equals(loaded.getOrDefault(entry.getKey(), "")));
        saveButton.setDisable(!changed);
        discardButton.setDisable(!changed);
    }

    /** The text of a field as the service expects it: numbers accept a decimal comma. */
    private static String typed(String key, TextField field) {
        String text = field.getText() == null ? "" : field.getText().trim();
        return SettingsService.BUSINESS_NAME.equals(key) ? text : text.replace(',', '.');
    }

    /** Shows 30 instead of 30.0, but keeps 12.5. */
    private static String number(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
