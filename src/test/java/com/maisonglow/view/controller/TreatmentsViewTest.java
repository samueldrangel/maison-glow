package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.TreatmentData;
import com.maisonglow.util.CurrencyFormatter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

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
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        return FxTest.onFxThread(() -> {
            Parent root = ViewLoader.load(TreatmentsViewTest.class.getResource("/com/maisonglow/view/" + resource), context);
            new Scene(root, 1100, 700);
            root.applyCss();
            root.layout();
            return root;
        });
    }

    private Parent open() throws Exception {
        return open("TreatmentsPane.fxml");
    }

    @SuppressWarnings("unchecked")
    private static List<String> names(Parent root) throws Exception {
        TableView<Treatment> table = (TableView<Treatment>) root.lookup("#treatmentsTable");
        return FxTest.onFxThread(() -> table.getItems().stream().map(Treatment::getName).toList());
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
    @DisplayName("shows the duration in minutes and the price in pesos")
    void formatsDurationAndPrice() throws Exception {
        Parent root = open();

        List<String> cells = FxTest.onFxThread(() -> root.lookupAll(".table-cell").stream()
            .map(node -> ((javafx.scene.control.Labeled) node).getText()).filter(text -> text != null).toList());

        assertThat(cells).contains("50 min", CurrencyFormatter.format(new BigDecimal("45000")));
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
    }

    @Test
    @DisplayName("the tab opens inside Profesionales y servicios, with its own controller")
    void opensInsideTheView() throws Exception {
        Parent root = open("ProfessionalsView.fxml");

        assertThat(names(root)).hasSize(2);
        assertThat(root.lookupAll(".tab")).hasSize(1);
    }
}
