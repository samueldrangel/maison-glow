package com.maisonglow.view;

import com.maisonglow.AppContext;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Settings view tests")
class SettingsViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = new AppContext(data);
    }

    private Parent open() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(SettingsViewTest.class.getResource("/com/maisonglow/view/SettingsView.fxml"), context);
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private static TextField field(Parent root, String id) {
        return (TextField) root.lookup("#" + id);
    }

    private static void type(Parent root, String id, String text) throws Exception {
        FxTest.onFxThread(() -> {
            field(root, id).setText(text);
            return null;
        });
    }

    private static void press(Parent root, String id) throws Exception {
        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#" + id)).fire();
            return null;
        });
    }

    private static String message(Parent root, String id) throws Exception {
        Label label = (Label) root.lookup("#" + id);
        return FxTest.onFxThread(() -> label.isVisible() ? label.getText() : "");
    }

    private String file() throws Exception {
        return Files.readString(data.resolve("settings.txt"));
    }

    @Test
    @DisplayName("shows the values saved in settings.txt and keeps the buttons off until something changes")
    void showsSavedValues() throws Exception {
        Parent root = open();

        assertThat(field(root, "businessField").getText()).isEqualTo("Maison Glow");
        assertThat(field(root, "depositField").getText()).isEqualTo("30");
        assertThat(field(root, "noticeField").getText()).isEqualTo("2");
        assertThat(field(root, "reminderField").getText()).isEqualTo("24");
        assertThat(field(root, "noShowField").getText()).isEqualTo("3");
        assertThat(field(root, "windowField").getText()).isEqualTo("30");
        assertThat(field(root, "topField").getText()).isEqualTo("5");
        assertThat(field(root, "discountField").getText()).isEqualTo("15");
        assertThat(root.lookup("#saveButton").isDisabled()).isTrue();
        assertThat(root.lookup("#discardButton").isDisabled()).isTrue();
    }

    @Test
    @DisplayName("a saved value is written to settings.txt and read back by the service")
    void savesToTheFile() throws Exception {
        Parent root = open();

        type(root, "depositField", "40");
        type(root, "windowField", "60");
        assertThat(root.lookup("#saveButton").isDisabled()).isFalse();
        press(root, "saveButton");

        assertThat(file()).contains("deposit_percentage|40|").contains("low_rotation_window_days|60|");
        assertThat(context.settings().depositPercentage()).isEqualTo(40);
        assertThat(root.lookup("#saveButton").isDisabled()).isTrue();
    }

    @Test
    @DisplayName("accepts a decimal comma in a percentage")
    void acceptsDecimalComma() throws Exception {
        Parent root = open();

        type(root, "discountField", "12,5");
        press(root, "saveButton");

        assertThat(context.settings().lowRotationDiscountPercentage()).isEqualTo(12.5);
    }

    @Test
    @DisplayName("a percentage out of range is rejected under its field and the file does not change")
    void rejectsPercentageOutOfRange() throws Exception {
        Parent root = open();
        String before = file();

        type(root, "depositField", "150");
        press(root, "saveButton");

        assertThat(message(root, "depositMessage")).isEqualTo("El parámetro «Anticipo» debe estar entre 0 y 100.");
        assertThat(field(root, "depositField").getStyleClass()).contains("field-error");
        assertThat(file()).isEqualTo(before);
    }

    @Test
    @DisplayName("a value that is not a number is rejected under its own field")
    void rejectsText() throws Exception {
        Parent root = open();

        type(root, "noShowField", "muchas");
        press(root, "saveButton");

        assertThat(message(root, "noShowMessage")).contains("número entero");
        assertThat(message(root, "depositMessage")).isEmpty();
    }

    @Test
    @DisplayName("a rejected value leaves no half-saved screen: the valid ones are put back")
    void rollsBackWhenOneFails() throws Exception {
        Parent root = open();
        String before = file();

        type(root, "depositField", "40");
        type(root, "topField", "0");
        press(root, "saveButton");

        assertThat(message(root, "topMessage")).contains("al menos 1");
        assertThat(file()).isEqualTo(before);
        assertThat(context.settings().depositPercentage()).isEqualTo(30);
    }

    @Test
    @DisplayName("a blank business name is rejected")
    void rejectsBlankName() throws Exception {
        Parent root = open();

        type(root, "businessField", "   ");
        press(root, "saveButton");

        assertThat(message(root, "businessMessage")).isNotEmpty();
        assertThat(file()).contains("business_name|Maison Glow|");
    }

    @Test
    @DisplayName("discarding brings back the saved values")
    void discards() throws Exception {
        Parent root = open();

        type(root, "businessField", "Otro nombre");
        press(root, "discardButton");

        assertThat(field(root, "businessField").getText()).isEqualTo("Maison Glow");
        assertThat(root.lookup("#saveButton").isDisabled()).isTrue();
    }
}
