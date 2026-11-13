package com.maisonglow.view;

import com.maisonglow.exception.BusinessRuleException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("List + form template tests")
class ListFormTemplateTest {

    private static final String TEMPLATE = "/com/maisonglow/view/templates/ListFormTemplate.fxml";
    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    private static Parent load() throws Exception {
        return FxTest.onFxThread(() -> {
            try {
                Parent root = FXMLLoader.load(ListFormTemplateTest.class.getResource(TEMPLATE));
                new Scene(new StackPane(root), 1100, 700);
                root.applyCss();
                root.layout();
                return root;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Test
    @DisplayName("loads with the list, the form and its buttons")
    void loadsWithItsParts() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        Parent root = load();

        assertThat(root.lookup("#table")).isInstanceOf(TableView.class);
        assertThat(root.lookup("#nameField")).isInstanceOf(TextField.class);
        assertThat(root.lookup("#categoryBox")).isInstanceOf(ComboBox.class);
        assertThat(root.lookup("#saveButton").getStyleClass()).contains("button-primary");
        assertThat(root.lookup("#cancelButton").getStyleClass()).contains("button-secondary");
    }

    @Test
    @DisplayName("keeps the palette when it sits inside another container, as in Scene Builder")
    void keepsThePalette() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        Region card = (Region) load().lookup(".form-card");

        assertThat(card.getBackground().getFills().get(0).getFill()).isEqualTo(Color.web("#fefefc"));
    }

    @Test
    @DisplayName("shows a service error under the right field with one registration per field")
    void showsErrorsUnderTheirField() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        Parent root = load();
        FieldValidator fields = new FieldValidator();
        TextField name = (TextField) root.lookup("#nameField");
        Label nameMessage = (Label) root.lookup("#nameMessage");
        Label priceMessage = (Label) root.lookup("#priceMessage");
        FxTest.onFxThread(() -> {
            fields.field(name, nameMessage, "nombre", "llamado");
            fields.field((TextField) root.lookup("#priceField"), priceMessage, "precio");
            return null;
        });
        assertThat(nameMessage.isVisible()).as("hidden once registered").isFalse();

        boolean marked = FxTest.onFxThread(() -> fields.show(new BusinessRuleException("Ya existe un producto llamado Shampoo.")));

        assertThat(marked).isTrue();
        assertThat(name.getStyleClass()).contains("field-error");
        assertThat(nameMessage.getText()).isEqualTo("Ya existe un producto llamado Shampoo.");
        assertThat(priceMessage.isVisible()).isFalse();
    }
}
