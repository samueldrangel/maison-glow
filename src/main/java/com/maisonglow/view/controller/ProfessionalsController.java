package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Professional;
import com.maisonglow.model.Treatment;
import com.maisonglow.service.ProfessionalService;
import com.maisonglow.service.TreatmentService;
import com.maisonglow.util.DateFormats;
import com.maisonglow.util.Initials;
import com.maisonglow.util.TextSearch;
import com.maisonglow.view.AlertPresenter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Objects;

/**
 * Controller of {@code ProfessionalsPane.fxml}: the professionals tab of "Profesionales y tratamientos"
 * (mockup M3). Each professional is a card with the name, specialty, working hours and state; the list can
 * be searched and filtered by the treatment a professional offers. It only talks to
 * {@link ProfessionalService} and {@link TreatmentService}, never to a DAO.
 */
public final class ProfessionalsController {

    /** The entry of the treatment filter that lets every professional through. */
    private static final Treatment ALL_TREATMENTS = new Treatment(0, "Todos los tratamientos", "", 1, 1);

    private final ProfessionalService professionals;
    private final TreatmentService treatments;
    private final ObservableList<Professional> all = FXCollections.observableArrayList();
    private final FilteredList<Professional> shown = new FilteredList<>(all);
    private AlertPresenter alerts;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Treatment> treatmentFilter;
    @FXML
    private FlowPane cardsPane;
    @FXML
    private Label countLabel;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public ProfessionalsController(AppContext context) {
        Objects.requireNonNull(context, "context");
        this.professionals = context.professionals();
        this.treatments = context.treatments();
    }

    /**
     * Replaces the presenter of dialogs, which the tests use so that no real window is opened.
     *
     * @param presenter the presenter to use from now on
     */
    public void useAlerts(AlertPresenter presenter) {
        this.alerts = Objects.requireNonNull(presenter, "presenter");
    }

    @FXML
    private void initialize() {
        alerts = new AlertPresenter(cardsPane);
        setUpFilters();
        refresh();
    }

    private void setUpFilters() {
        treatmentFilter.setConverter(new StringConverter<>() {
            @Override
            public String toString(Treatment treatment) {
                return treatment == null ? "" : treatment.getName();
            }

            @Override
            public Treatment fromString(String text) {
                return null;
            }
        });
        searchField.textProperty().addListener((observable, before, text) -> applyFilters());
        treatmentFilter.valueProperty().addListener((observable, before, treatment) -> applyFilters());
    }

    private void applyFilters() {
        String text = searchField.getText();
        Treatment wanted = treatmentFilter.getValue();
        shown.setPredicate(professional ->
            (TextSearch.contains(professional.getName(), text) || TextSearch.contains(professional.getSpecialty(), text))
                && (wanted == null || wanted == ALL_TREATMENTS || professional.getTreatments().contains(wanted)));
        showCards();
    }

    /** Reads the professionals and treatments again, so the cards show what is stored. */
    void refresh() {
        try {
            all.setAll(professionals.listAll());
            Treatment chosen = treatmentFilter.getValue();
            List<Treatment> options = new java.util.ArrayList<>(List.of(ALL_TREATMENTS));
            options.addAll(treatments.listAll());
            treatmentFilter.getItems().setAll(options);
            treatmentFilter.setValue(options.stream().filter(option -> option.equals(chosen)).findFirst().orElse(ALL_TREATMENTS));
            showCards();
        } catch (DataAccessException e) {
            alerts.showError(e);
        }
    }

    private void showCards() {
        cardsPane.getChildren().setAll(shown.stream().map(this::card).toList());
        countLabel.setText("Mostrando " + shown.size() + " de " + all.size()
            + (all.size() == 1 ? " profesional" : " profesionales"));
    }

    /** One professional as a card: medallion with the initials, name, specialty, hours, services and state. */
    private Node card(Professional professional) {
        Label initials = new Label(Initials.of(professional.getName()));
        initials.getStyleClass().add("pro-initials");
        StackPane medallion = new StackPane(initials);
        medallion.getStyleClass().add("pro-medallion");
        medallion.setMinSize(64, 64);
        medallion.setPrefSize(64, 64);
        medallion.setMaxSize(64, 64);
        int services = professional.getTreatments().size();
        VBox card = new VBox(actions(professional), medallion,
            text(professional.getName(), "pro-name"),
            text(professional.getSpecialty(), "pro-specialty"),
            text(DateFormats.time(professional.getWorkStart()) + " - " + DateFormats.time(professional.getWorkEnd()), "pro-hours"),
            text(services == 0 ? "Sin tratamientos" : services + (services == 1 ? " tratamiento" : " tratamientos"), "pro-hours"),
            status(professional));
        card.getStyleClass().addAll("card", "pro-card");
        if (!professional.isActive()) {
            card.getStyleClass().add("pro-card-inactive");
        }
        return card;
    }

    /** The row at the top of a card with its action buttons. */
    private Node actions(Professional professional) {
        HBox row = new HBox(2);
        row.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return row;
    }

    private static Label text(String text, String style) {
        Label label = new Label(text);
        label.getStyleClass().add(style);
        return label;
    }

    private static Label status(Professional professional) {
        Label chip = new Label(professional.isActive() ? "Activo" : "Inactivo");
        Circle dot = new Circle(3.5);
        dot.getStyleClass().add("chip-dot");
        chip.setGraphic(dot);
        chip.getStyleClass().add(professional.isActive() ? "chip-success" : "chip-neutral");
        return chip;
    }
}
