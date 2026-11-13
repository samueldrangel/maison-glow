package com.maisonglow.view;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Icons tests")
class IconsTest {

    private static boolean toolkit;

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @Test
    @DisplayName("every icon is a vector that takes its colour from the CSS")
    void everyIconIsAStyledVector() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        for (Icons.Kind kind : Icons.Kind.values()) {
            SVGPath icon = FxTest.onFxThread(() -> Icons.line(kind));
            assertThat(icon.getContent()).as(kind.name()).isNotBlank();
            assertThat(icon.getStyleClass()).as(kind.name()).contains("line-icon");
        }
    }

    @Test
    @DisplayName("each kind of dialog has its own icon and tone")
    void eachKindOfDialogHasItsOwnLook() {
        assertThat(Icons.kindOf(AlertType.ERROR)).isEqualTo(Icons.Kind.ERROR);
        assertThat(Icons.kindOf(AlertType.WARNING)).isEqualTo(Icons.Kind.WARNING);
        assertThat(Icons.kindOf(AlertType.INFORMATION)).isEqualTo(Icons.Kind.INFO);
        assertThat(Icons.kindOf(AlertType.CONFIRMATION)).isEqualTo(Icons.Kind.QUESTION);
        assertThat(Icons.toneOf(AlertType.ERROR)).isEqualTo("tone-error");
        assertThat(Icons.toneOf(AlertType.WARNING)).isEqualTo("tone-warning");
        assertThat(Icons.toneOf(AlertType.CONFIRMATION)).isEqualTo("tone-question");
    }

    @Test
    @DisplayName("a small icon takes exactly the room it is given")
    void aSmallIconTakesTheRoomItIsGiven() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        StackPane small = FxTest.onFxThread(() -> Icons.small(Icons.Kind.ALERT_CIRCLE, 15));

        assertThat(small.getPrefWidth()).isEqualTo(15);
        assertThat(small.getMaxHeight()).isEqualTo(15);
        assertThat(small.getChildren().get(0).getScaleX()).isEqualTo(15.0 / 24);
    }

    @Test
    @DisplayName("the medallion holds the icon of its kind and the tone class")
    void theMedallionHoldsTheIcon() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");

        StackPane medallion = FxTest.onFxThread(() -> Icons.medallion(AlertType.WARNING));

        assertThat(medallion.getStyleClass()).contains("dialog-medallion", "tone-warning");
        assertThat(medallion.getChildren()).hasSize(1);
        assertThat(((SVGPath) medallion.getChildren().get(0)).getContent()).isEqualTo(
            FxTest.onFxThread(() -> Icons.line(Icons.Kind.WARNING)).getContent());
    }
}
