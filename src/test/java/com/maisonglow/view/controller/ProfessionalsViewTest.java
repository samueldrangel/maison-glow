package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.service.WorkSchedule;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
        context = new AppContext(data);
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
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(ProfessionalsViewTest.class.getResource("/com/maisonglow/view/ProfessionalsPane.fxml"), context);
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
}
