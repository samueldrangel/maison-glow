package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Treatments tab tests")
class TreatmentsViewTest {

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
        context.treatments().register(new TreatmentData("Manicura gel", "Esmaltado semipermanente", 45000, 50));
        Treatment cut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        context.treatments().deactivate(cut.getId());
    }

    private Parent open(String resource) throws Exception {
        return open(resource, true);
    }

    /** Opens a screen; a confirmation is answered as the user would, with "Confirmar" or by cancelling. */
    private Parent open(String resource, boolean confirm) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<Object> loaded = ViewLoader.loadWithController(
                TreatmentsViewTest.class.getResource("/com/maisonglow/view/" + resource), context);
            if (loaded.controller() instanceof TreatmentsController controller) {
                controller.useAlerts(new AlertPresenter(() -> null, alert -> alert.getButtonTypes().stream()
                    .filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst()));
            }
            Parent root = loaded.root();
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private Parent open() throws Exception {
        return open("TreatmentsPane.fxml");
    }

    /** The names on the cards, in the order they are shown. */
    private static List<String> names(Parent root) throws Exception {
        return texts(root, ".tre-name");
    }

    private static List<String> texts(Parent root, String style) throws Exception {
        return FxTest.onFxThread(() -> root.lookupAll(style).stream().map(node -> ((Label) node).getText()).toList());
    }

    private static void fill(Parent root, String name, String price, String minutes) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#nameField")).setText(name);
            ((TextField) root.lookup("#priceField")).setText(price);
            ((TextField) root.lookup("#durationField")).setText(minutes);
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

    /** Presses the icon button of the table that has that description, as a user would click it. */
    private static void click(Parent root, String description) throws Exception {
        FxTest.onFxThread(() -> {
            root.applyCss();
            root.layout();
            root.lookup("#cardsPane").lookupAll(".icon-button").stream().map(Button.class::cast)
                .filter(button -> description.equals(button.getAccessibleText())).findFirst().orElseThrow().fire();
            root.applyCss();
            root.layout();
            return null;
        });
    }

    /** The animation runs on its own; this waits for the panel to reach the state the test expects. */
    private static boolean panelManaged(Parent root, boolean expected) throws Exception {
        ScrollPane panel = (ScrollPane) root.lookup("#formPanel");
        Supplier<Boolean> reading = panel::isManaged;
        Predicate<Boolean> done = managed -> managed == expected;
        boolean value = FxTest.onFxThread(reading);
        for (int attempt = 0; attempt < 80 && !done.test(value); attempt++) {
            Thread.sleep(25);
            value = FxTest.onFxThread(reading);
        }
        return value;
    }

    @Test
    @DisplayName("lists every treatment, active or not, with its duration, price and state")
    void listsTreatments() throws Exception {
        Parent root = open();

        assertThat(names(root)).containsExactlyInAnyOrder("Manicura gel", "Corte y peinado");
        List<String> chips = FxTest.onFxThread(() -> java.util.stream.Stream.concat(root.lookupAll(".chip-success").stream(),
            root.lookupAll(".chip-neutral").stream()).map(node -> ((Label) node).getText()).toList());
        assertThat(chips).containsExactlyInAnyOrder("Activo", "Inactivo");
    }

    @Test
    @DisplayName("each card shows the duration in minutes, the price in pesos and what the treatment includes")
    void showsTheDetails() throws Exception {
        Parent root = open();

        assertThat(texts(root, ".tre-meta")).contains("50 min", "60 min");
        assertThat(texts(root, ".tre-price")).contains(CurrencyFormatter.format(new BigDecimal("45000")));
        assertThat(texts(root, ".tre-description")).containsExactly("Esmaltado semipermanente");
        assertThat(texts(root, ".tre-description-empty")).containsExactly("Sin descripción");
        assertThat(((Label) root.lookup("#countLabel")).getText()).isEqualTo("Mostrando 2 de 2 tratamientos");
    }

    @Test
    @DisplayName("the cards come in alphabetical order and an inactive one is dimmed")
    void ordersAndDimsTheInactive() throws Exception {
        Parent root = open();

        assertThat(names(root)).containsExactly("Corte y peinado", "Manicura gel");
        assertThat(FxTest.onFxThread(() -> ((FlowPane) root.lookup("#cardsPane")).getChildren().get(0).getStyleClass()))
            .contains("tre-card-inactive");
    }

    @Test
    @DisplayName("the search ignores case and accents")
    void searches() throws Exception {
        Parent root = open();

        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#searchField")).setText("MANICURA");
            return null;
        });

        assertThat(names(root)).containsExactly("Manicura gel");
        assertThat(((Label) root.lookup("#countLabel")).getText()).isEqualTo("Mostrando 1 de 2 tratamientos");
    }

    @Test
    @DisplayName("both tabs open inside Profesionales y servicios, each with its own controller")
    void opensInsideTheView() throws Exception {
        Parent root = open("ProfessionalsView.fxml");

        assertThat(names(root)).hasSize(2);
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".tab-label").stream().map(node -> ((Label) node).getText()).toList()))
            .containsExactly("Profesionales", "Tratamientos");
    }

    @Test
    @DisplayName("registering a treatment adds it to the table, closes the form and keeps the data")
    void registers() throws Exception {
        Parent root = open();

        press(root, "newButton");
        fill(root, "Limpieza facial", "38000", "45");
        press(root, "saveButton");

        assertThat(names(root)).contains("Limpieza facial");
        assertThat(panelManaged(root, false)).isFalse();
        Treatment saved = context.treatments().listAll().stream().filter(t -> t.getName().equals("Limpieza facial")).findFirst().orElseThrow();
        assertThat(saved.getPrice()).isEqualByComparingTo("38000");
        assertThat(saved.getDurationMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("a description of several lines is saved and comes back in the form when editing")
    void keepsAMultiLineDescription() throws Exception {
        Parent root = open();
        press(root, "newButton");
        fill(root, "Limpieza facial", "38000", "45");
        FxTest.onFxThread(() -> {
            ((TextArea) root.lookup("#descriptionField")).setText("Incluye:\nlimpieza profunda\nmascarilla");
            return null;
        });
        press(root, "saveButton");

        click(root, "Editar Limpieza facial");

        assertThat(((TextArea) root.lookup("#descriptionField")).getText()).isEqualTo("Incluye:\nlimpieza profunda\nmascarilla");
    }

    @Test
    @DisplayName("each wrong field is reported under its own field and nothing is saved")
    void reportsEachField() throws Exception {
        Parent root = open();
        press(root, "newButton");

        fill(root, "", "38000", "45");
        press(root, "saveButton");
        assertThat(message(root, "nameMessage")).isNotEmpty();

        fill(root, "Limpieza facial", "0", "45");
        press(root, "saveButton");
        assertThat(message(root, "priceMessage")).isNotEmpty();

        fill(root, "Limpieza facial", "38000", "0");
        press(root, "saveButton");
        assertThat(message(root, "durationMessage")).isNotEmpty();

        fill(root, "Limpieza facial", "38000", "mucho");
        press(root, "saveButton");
        assertThat(message(root, "durationMessage")).isNotEmpty();
        assertThat(names(root)).hasSize(2);
        assertThat(panelManaged(root, true)).isTrue();
    }

    @Test
    @DisplayName("a repeated name is rejected under the name field")
    void rejectsRepeatedName() throws Exception {
        Parent root = open();
        press(root, "newButton");

        fill(root, "Manicura gel", "38000", "45");
        press(root, "saveButton");

        assertThat(message(root, "nameMessage")).contains("Manicura gel");
        assertThat(names(root)).hasSize(2);
    }

    @Test
    @DisplayName("the pencil fills the form and saving the changes updates the treatment")
    void edits() throws Exception {
        Parent root = open();

        click(root, "Editar Manicura gel");
        assertThat(((TextField) root.lookup("#priceField")).getText()).isEqualTo("45000");
        assertThat(((Label) root.lookup("#formTitle")).getText()).isEqualTo("Editar tratamiento");
        fill(root, "Manicura gel", "48000", "55");
        press(root, "saveButton");

        Treatment saved = context.treatments().listAll().stream().filter(t -> t.getName().equals("Manicura gel")).findFirst().orElseThrow();
        assertThat(saved.getPrice()).isEqualByComparingTo("48000");
        assertThat(saved.getDurationMinutes()).isEqualTo(55);
        assertThat(panelManaged(root, false)).isFalse();
    }

    @Test
    @DisplayName("deactivating asks first, and a deactivated treatment can be reactivated")
    void deactivatesAndReactivates() throws Exception {
        Parent root = open();

        click(root, "Desactivar Manicura gel");
        assertThat(context.treatments().listActive()).isEmpty();

        click(root, "Reactivar Manicura gel");
        assertThat(context.treatments().listActive()).extracting(Treatment::getName).containsExactly("Manicura gel");
    }

    @Test
    @DisplayName("cancelling the confirmation keeps the treatment active")
    void keepsActiveWhenCancelled() throws Exception {
        Parent root = open("TreatmentsPane.fxml", false);

        click(root, "Desactivar Manicura gel");

        assertThat(context.treatments().listActive()).hasSize(1);
    }

    @Test
    @DisplayName("the form is hidden at first; Nuevo tratamiento brings it in and Cancelar takes it out and empties it")
    void drawerOpensAndCloses() throws Exception {
        Parent root = open();
        assertThat(root.lookup("#formPanel").isManaged()).isFalse();

        press(root, "newButton");
        assertThat(panelManaged(root, true)).isTrue();
        fill(root, "Limpieza facial", "38000", "45");

        press(root, "cancelButton");
        assertThat(panelManaged(root, false)).isFalse();
        assertThat(((TextField) root.lookup("#nameField")).getText()).isEmpty();
    }
}
