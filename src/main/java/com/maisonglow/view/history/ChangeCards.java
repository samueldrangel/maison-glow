package com.maisonglow.view.history;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Commerce.Change;
import com.maisonglow.service.ChangeHistoryService;
import com.maisonglow.util.DateFormats;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Draws one record of a history of changes as a card: its date, what kind it is, the notes, and the photos before and after
 * with a button to take them away. The same card is used by the administrator's history of a customer and by "Mis cambios" of
 * the customer, so both show a record in the same way. It only draws: which photos may be seen is decided by
 * {@link ChangeHistoryService#photo}, which checks that the person owns the record or is an authorized administrator.
 */
public final class ChangeCards {

    private ChangeCards() {
    }

    /**
     * Builds the card of a record.
     *
     * @param change       the record
     * @param changes      gives the photos of the record
     * @param removePhotos what to do when the person asks to take the photos away; the card offers the button only if it has photos
     * @return the card
     */
    public static Node card(Change change, ChangeHistoryService changes, Consumer<Change> removePhotos) {
        Objects.requireNonNull(change, "change");
        Label date = new Label(DateFormats.date(change.happenedOn()));
        date.getStyleClass().add("stat-value");
        Label kind = new Label(kindLabel(change));
        kind.getStyleClass().add(switch (change.kind()) {
            case SERVICE -> "chip-gold";
            case PERSONAL -> "chip-rose";
            case SIMULATION -> "chip-neutral";
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, date, kind, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        Label notes = new Label(change.notes());
        notes.getStyleClass().add("form-hint");
        notes.setWrapText(true);
        VBox card = new VBox(8, header, notes);
        card.getStyleClass().addAll("card", "form-card");
        HBox photos = new HBox(10);
        addPhoto(photos, change, changes, false, change.sourceKey() == null ? null : "Antes");
        addPhoto(photos, change, changes, true, change.resultKey() == null ? null : "Después");
        if (!photos.getChildren().isEmpty()) {
            card.getChildren().add(photos);
            Button remove = new Button("Retirar fotos");
            remove.getStyleClass().addAll("button-secondary", "button-compact");
            remove.setOnAction(event -> removePhotos.accept(change));
            card.getChildren().add(remove);
        }
        return card;
    }

    /** @return the words of the chip of a record: a simulation is always said to be one, never an attention of the business */
    public static String kindLabel(Change change) {
        return switch (change.kind()) {
            case SERVICE -> "Servicio del negocio";
            case PERSONAL -> "Cambio personal";
            case SIMULATION -> "Simulación (no es un resultado real)";
        };
    }

    private static void addPhoto(HBox photos, Change change, ChangeHistoryService changes, boolean result, String caption) {
        if (caption == null) {
            return;
        }
        try {
            Path file = changes.photo(change.id(), result);
            Image image = new Image(file.toUri().toString(), 180, 140, true, true);
            if (image.isError()) {
                return;
            }
            ImageView view = new ImageView(image);
            view.setPreserveRatio(true);
            Label label = new Label(caption);
            label.getStyleClass().add("stat-caption");
            photos.getChildren().add(new VBox(4, view, label));
        } catch (BusinessRuleException | DataAccessException e) {
            // A photo that is gone from the disk is simply not shown; the record and its notes stay.
        }
    }
}
