package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Professionals tab tests")
class ProfessionalsViewTest {

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private Treatment manicure;
    private Treatment haircut;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepareData() throws Exception {
        AppContext.seed(data);
        context = com.maisonglow.TestAccounts.open(data);
        manicure = context.treatments().register(new TreatmentData("Manicura gel", "", 45000, 50));
        haircut = context.treatments().register(new TreatmentData("Corte y peinado", "", 55000, 60));
        Professional camila = context.professionals().register(
            new ContactData("2001", "Camila Ruiz", "3001112233", "camila@correo.com"), "Estilista",
            new WorkSchedule(LocalTime.of(8, 0), LocalTime.of(17, 0)));
        context.professionals().register(new ContactData("2002", "Daniela Soto", "3124445566", "daniela@correo.com"),
            "Manicurista", new WorkSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0)));
        context.professionals().assignTreatment(camila.getId(), haircut.getId());
    }

    private Parent open() throws Exception {
        return open(true);
    }

    /** Opens the screen; a confirmation is answered as the user would, with "Confirmar" or by cancelling. */
    private Parent open(boolean confirm) throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<ProfessionalsController> loaded = ViewLoader.loadWithController(
                ProfessionalsViewTest.class.getResource("/com/maisonglow/view/ProfessionalsPane.fxml"), context);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> alert.getButtonTypes().stream()
                .filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst()));
            Parent root = loaded.root();
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    /** The names on the cards, in the order they are shown. */
    private static List<String> cards(Parent root) throws Exception {
        FlowPane pane = (FlowPane) root.lookup("#cardsPane");
        return FxTest.onFxThread(() -> pane.getChildren().stream()
            .map(card -> ((Label) card.lookup(".pro-name")).getText()).toList());
    }

    private static void type(Parent root, String text) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#searchField")).setText(text);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static void filterBy(Parent root, String treatment) throws Exception {
        ComboBox<Treatment> filter = (ComboBox<Treatment>) root.lookup("#treatmentFilter");
        FxTest.onFxThread(() -> {
            filter.getSelectionModel().select(filter.getItems().stream().filter(t -> t.getName().equals(treatment)).findFirst().orElseThrow());
            return null;
        });
    }

    private static void fill(Parent root, String document, String name, String phone, String email, String specialty,
                             String start, String end) throws Exception {
        FxTest.onFxThread(() -> {
            ((TextField) root.lookup("#documentField")).setText(document);
            ((TextField) root.lookup("#nameField")).setText(name);
            ((TextField) root.lookup("#phoneField")).setText(phone);
            ((TextField) root.lookup("#emailField")).setText(email);
            ((TextField) root.lookup("#specialtyField")).setText(specialty);
            ((TextField) root.lookup("#startField")).setText(start);
            ((TextField) root.lookup("#endField")).setText(end);
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

    /** Presses the icon button of a card that has that description, as a user would click it. */
    private static void click(Parent root, String description) throws Exception {
        FxTest.onFxThread(() -> {
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

    @SuppressWarnings("unchecked")
    private static void pick(Parent root, String treatment) throws Exception {
        ComboBox<Treatment> box = (ComboBox<Treatment>) root.lookup("#assignBox");
        FxTest.onFxThread(() -> {
            box.getSelectionModel().select(box.getItems().stream().filter(t -> t.getName().equals(treatment)).findFirst().orElseThrow());
            return null;
        });
    }

    /** The names of the services offered by a professional, as the store has them. */
    private List<String> offered(String document) throws Exception {
        return context.professionals().listAll().stream().filter(p -> p.getDocument().equals(document)).findFirst().orElseThrow()
            .getTreatments().stream().map(Treatment::getName).toList();
    }

    @Test
    @DisplayName("shows a card per professional with the specialty, the hours and the treatments")
    void showsCards() throws Exception {
        Parent root = open();

        assertThat(cards(root)).containsExactlyInAnyOrder("Camila Ruiz", "Daniela Soto");
        List<String> texts = FxTest.onFxThread(() -> root.lookupAll(".pro-specialty").stream()
            .map(node -> ((Label) node).getText()).toList());
        assertThat(texts).containsExactlyInAnyOrder("Estilista", "Manicurista");
        List<String> hours = FxTest.onFxThread(() -> root.lookupAll(".pro-hours").stream()
            .map(node -> ((Label) node).getText()).toList());
        assertThat(hours).contains("08:00 - 17:00", "09:00 - 18:00", "1 tratamiento", "Sin tratamientos");
        assertThat(((Label) root.lookup(".pro-initials")).getText()).isIn("CR", "DS");
        assertThat(((Label) root.lookup("#countLabel")).getText()).isEqualTo("Mostrando 2 de 2 profesionales");
    }

    @Test
    @DisplayName("the search finds by name or specialty ignoring case and accents")
    void searches() throws Exception {
        Parent root = open();

        type(root, "ESTILISTA");
        assertThat(cards(root)).containsExactly("Camila Ruiz");

        type(root, "daniela");
        assertThat(cards(root)).containsExactly("Daniela Soto");
        assertThat(((Label) root.lookup("#countLabel")).getText()).isEqualTo("Mostrando 1 de 2 profesionales");
    }

    @Test
    @DisplayName("filtering by a treatment keeps only the professionals who offer it, and Todos brings everyone back")
    void filtersByTreatment() throws Exception {
        Parent root = open();

        filterBy(root, "Corte y peinado");
        assertThat(cards(root)).containsExactly("Camila Ruiz");

        filterBy(root, "Manicura gel");
        assertThat(cards(root)).isEmpty();

        filterBy(root, "Todos los tratamientos");
        assertThat(cards(root)).hasSize(2);
    }

    @Test
    @DisplayName("registering a professional adds a card, closes the form and keeps the working hours")
    void registers() throws Exception {
        Parent root = open();

        press(root, "newButton");
        assertThat(panelManaged(root, true)).isTrue();
        fill(root, "2003", "María López", "3207778899", "maria@correo.com", "Esteticista", "08:30", "16:30");
        press(root, "saveButton");

        assertThat(cards(root)).contains("María López");
        assertThat(panelManaged(root, false)).isFalse();
        Professional saved = context.professionals().listAll().stream().filter(p -> p.getName().equals("María López")).findFirst().orElseThrow();
        assertThat(saved.getWorkStart()).isEqualTo(LocalTime.of(8, 30));
        assertThat(saved.getWorkEnd()).isEqualTo(LocalTime.of(16, 30));
    }

    @Test
    @DisplayName("each wrong field is reported under its own field and nothing is saved")
    void reportsEachField() throws Exception {
        Parent root = open();
        press(root, "newButton");

        fill(root, "", "María López", "3207778899", "maria@correo.com", "Esteticista", "08:00", "17:00");
        press(root, "saveButton");
        assertThat(message(root, "documentMessage")).isNotEmpty();

        fill(root, "2003", "María López", "12", "maria@correo.com", "Esteticista", "08:00", "17:00");
        press(root, "saveButton");
        assertThat(message(root, "phoneMessage")).isNotEmpty();

        fill(root, "2003", "María López", "3207778899", "sin-arroba", "Esteticista", "08:00", "17:00");
        press(root, "saveButton");
        assertThat(message(root, "emailMessage")).isNotEmpty();

        fill(root, "2003", "María López", "3207778899", "maria@correo.com", "", "08:00", "17:00");
        press(root, "saveButton");
        assertThat(message(root, "specialtyMessage")).isNotEmpty();
        assertThat(cards(root)).hasSize(2);
    }

    @Test
    @DisplayName("a working day that starts after it ends, or a time that is not HH:mm, is rejected under the hours")
    void rejectsABadWorkingDay() throws Exception {
        Parent root = open();
        press(root, "newButton");

        fill(root, "2003", "María López", "3207778899", "maria@correo.com", "Esteticista", "18:00", "09:00");
        press(root, "saveButton");
        assertThat(message(root, "scheduleMessage")).contains("anterior");

        fill(root, "2003", "María López", "3207778899", "maria@correo.com", "Esteticista", "mañana", "17:00");
        press(root, "saveButton");
        assertThat(message(root, "scheduleMessage")).contains("HH:mm");
        assertThat(cards(root)).hasSize(2);
        assertThat(panelManaged(root, true)).isTrue();
    }

    @Test
    @DisplayName("a repeated document is rejected under the document field")
    void rejectsRepeatedDocument() throws Exception {
        Parent root = open();
        press(root, "newButton");

        fill(root, "2001", "Otra Persona", "3001110000", "otra@correo.com", "Estilista", "08:00", "17:00");
        press(root, "saveButton");

        assertThat(message(root, "documentMessage")).contains("2001");
    }

    @Test
    @DisplayName("the pencil fills the form, keeps the document fixed and saves new data and hours")
    void edits() throws Exception {
        Parent root = open();

        click(root, "Editar Camila Ruiz");
        assertThat(((TextField) root.lookup("#documentField")).isDisabled()).isTrue();
        assertThat(((TextField) root.lookup("#startField")).getText()).isEqualTo("08:00");
        assertThat(((Label) root.lookup("#formTitle")).getText()).isEqualTo("Editar profesional");
        fill(root, "2001", "Camila Ruiz Peña", "3001112233", "camila@correo.com", "Estilista senior", "07:30", "16:00");
        press(root, "saveButton");

        Professional saved = context.professionals().listAll().stream().filter(p -> p.getDocument().equals("2001")).findFirst().orElseThrow();
        assertThat(saved.getName()).isEqualTo("Camila Ruiz Peña");
        assertThat(saved.getSpecialty()).isEqualTo("Estilista senior");
        assertThat(saved.getWorkStart()).isEqualTo(LocalTime.of(7, 30));
        assertThat(saved.getWorkEnd()).isEqualTo(LocalTime.of(16, 0));
        assertThat(panelManaged(root, false)).isFalse();
    }

    @Test
    @DisplayName("deactivating asks first and the card shows Inactivo; reactivating brings it back")
    void deactivatesAndReactivates() throws Exception {
        Parent root = open(true);

        click(root, "Desactivar Camila Ruiz");
        assertThat(context.professionals().listAll().stream().filter(p -> !p.isActive()).map(Professional::getName)).containsExactly("Camila Ruiz");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".pro-card-inactive").size())).isEqualTo(1);

        click(root, "Reactivar Camila Ruiz");
        assertThat(context.professionals().listAll().stream().allMatch(Professional::isActive)).isTrue();
    }

    @Test
    @DisplayName("cancelling the confirmation keeps the professional active")
    void keepsActiveWhenCancelled() throws Exception {
        Parent root = open(false);

        click(root, "Desactivar Camila Ruiz");

        assertThat(context.professionals().listAll().stream().allMatch(Professional::isActive)).isTrue();
    }

    @Test
    @DisplayName("the form is hidden at first; Cancelar takes it out and empties it")
    void drawerOpensAndCloses() throws Exception {
        Parent root = open();
        assertThat(root.lookup("#formPanel").isManaged()).isFalse();

        press(root, "newButton");
        fill(root, "2003", "María López", "3207778899", "maria@correo.com", "Esteticista", "08:00", "17:00");
        press(root, "cancelButton");

        assertThat(panelManaged(root, false)).isFalse();
        assertThat(((TextField) root.lookup("#nameField")).getText()).isEmpty();
    }

    @Test
    @DisplayName("the services card appears only when editing, with the services already offered")
    void servicesCardShowsWhileEditing() throws Exception {
        Parent root = open();
        assertThat(root.lookup("#servicesCard").isManaged()).isFalse();

        click(root, "Editar Camila Ruiz");

        assertThat(root.lookup("#servicesCard").isManaged()).isTrue();
        assertThat(FxTest.onFxThread(() -> ((javafx.scene.layout.FlowPane) root.lookup("#servicesChips")).getChildren().stream()
            .map(node -> ((javafx.scene.control.Labeled) node).getText().trim()).toList())).containsExactly("Corte y peinado  ×".trim());
        press(root, "cancelButton");
        assertThat(root.lookup("#servicesCard").isManaged()).isFalse();
    }

    @Test
    @DisplayName("assigning a service adds it to the professional and to the card")
    void assignsAService() throws Exception {
        Parent root = open();
        click(root, "Editar Daniela Soto");

        pick(root, "Manicura gel");
        press(root, "assignButton");

        assertThat(offered("2002")).containsExactly("Manicura gel");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".pro-hours").stream().map(node -> ((Label) node).getText()).toList()))
            .doesNotContain("Sin tratamientos");
    }

    @Test
    @DisplayName("assigning an inactive service shows the error under the picker and changes nothing")
    void rejectsAnInactiveService() throws Exception {
        context.treatments().deactivate(manicure.getId());
        Parent root = open();
        click(root, "Editar Daniela Soto");

        pick(root, "Manicura gel");
        press(root, "assignButton");

        assertThat(message(root, "assignMessage")).contains("no está activo");
        assertThat(offered("2002")).isEmpty();
    }

    @Test
    @DisplayName("assigning a service the professional already offers, or none at all, shows the error")
    void rejectsRepeatedOrMissing() throws Exception {
        Parent root = open();
        click(root, "Editar Camila Ruiz");

        press(root, "assignButton");
        assertThat(message(root, "assignMessage")).isNotEmpty();

        pick(root, "Corte y peinado");
        press(root, "assignButton");
        assertThat(message(root, "assignMessage")).contains("ya ofrece");
        assertThat(offered("2001")).containsExactly("Corte y peinado");
    }

    @Test
    @DisplayName("the chip of an offered service takes it away")
    void removesAService() throws Exception {
        Parent root = open();
        click(root, "Editar Camila Ruiz");

        FxTest.onFxThread(() -> {
            ((Button) root.lookup("#servicesChips").lookupAll(".chip-rose").stream().findFirst().orElseThrow()).fire();
            return null;
        });

        assertThat(offered("2001")).isEmpty();
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".pro-hours").stream().map(node -> ((Label) node).getText()).toList()))
            .doesNotContain("1 tratamiento");
    }
}
