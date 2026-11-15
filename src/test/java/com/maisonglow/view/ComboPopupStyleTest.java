package com.maisonglow.view;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Combo box popup style tests")
class ComboPopupStyleTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    /** The popup of a combo box is a list inside a node with the class combo-box-popup; it is built the same way here. */
    private static ListView<String> popupList() throws Exception {
        return FxTest.onFxThread(() -> {
            ListView<String> list = new ListView<>(FXCollections.observableArrayList("Labial · 3 uds.", "Base Líquida · 24 uds."));
            list.getSelectionModel().select(0);
            StackPane popup = new StackPane(list);
            popup.getStyleClass().addAll("combo-box-popup", "view-root");
            Scene scene = new Scene(popup, 300, 200);
            scene.getStylesheets().add(ComboPopupStyleTest.class.getResource("/com/maisonglow/view/styles.css").toExternalForm());
            popup.applyCss();
            popup.layout();
            return list;
        });
    }

    @Test
    @DisplayName("the chosen option is soft rose, not the default blue of JavaFX")
    void chosenOptionIsRose() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        ListView<String> list = popupList();

        ListCell<?> chosen = FxTest.onFxThread(() -> list.lookupAll(".list-cell").stream().map(ListCell.class::cast)
            .filter(ListCell::isSelected).findFirst().orElseThrow());

        assertThat(chosen.getBackground().getFills().get(0).getFill()).isEqualTo(Color.web("#f3dfe1"));
    }

    @Test
    @DisplayName("the list is a rounded light card")
    void listIsACard() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        ListView<String> list = popupList();

        assertThat(list.getBackground().getFills().get(0).getFill()).isEqualTo(Color.web("#fefefc"));
        assertThat(list.getBackground().getFills().get(0).getRadii().getTopLeftHorizontalRadius()).isGreaterThan(0);
    }
}
