package com.maisonglow.view;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Objects;

/**
 * A menu entry with sub-entries (for example Citas with Agenda and
 * Profesionales). Its sub-entries are only shown while one of them is the
 * current screen, and they open and close with a short slide.
 *
 * <p>A group is declared in the FXML as a {@code VBox} with the style class
 * {@code nav-group-box}, a header button and a {@code VBox} with the class
 * {@code nav-sub-list}; any menu entry that needs more options repeats that
 * structure, with no extra code.
 */
public final class NavGroup {

    private static final String OPEN = "open";
    private static final Duration SLIDE = Duration.millis(220);

    private final VBox box;
    private final VBox list;
    private final Node chevron;
    private Timeline running;
    private boolean open = true;

    /**
     * Wraps the {@code nav-group-box} of the FXML.
     *
     * @param box the group container
     */
    public NavGroup(VBox box) {
        this.box = Objects.requireNonNull(box, "box");
        this.list = (VBox) Objects.requireNonNull(box.lookup(".nav-sub-list"), "nav-sub-list");
        this.chevron = box.lookup(".nav-chevron-box");
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(list.widthProperty());
        clip.heightProperty().bind(list.heightProperty());
        list.setClip(clip);
        list.setMinHeight(0);
    }

    /** @return true when the toggle is one of the sub-entries of this group */
    public boolean contains(Toggle toggle) {
        return toggle instanceof Node node && list.getChildren().contains(node);
    }

    /** @return true when the node is the group or anything inside it (its header, for example) */
    public boolean owns(Node node) {
        for (Node current = node; current != null; current = current.getParent()) {
            if (current == box) {
                return true;
            }
        }
        return false;
    }

    /** Goes to the first sub-entry, which is where the header button of the group leads. */
    public void openFirst() {
        ((ToggleButton) list.getChildren().get(0)).fire();
    }

    /**
     * Opens or closes the sub-entries.
     *
     * @param open whether they must be visible
     * @param animate false to jump to the final state (used when the window opens)
     */
    public void setOpen(boolean open, boolean animate) {
        if (animate && open == this.open) {
            return;
        }
        this.open = open;
        if (running != null) {
            running.stop();
        }
        if (open) {
            if (!box.getStyleClass().contains(OPEN)) {
                box.getStyleClass().add(OPEN);
            }
            list.setVisible(true);
        } else {
            box.getStyleClass().remove(OPEN);
        }
        // Hidden nodes do not get their CSS, so apply it before measuring the full height.
        list.applyCss();
        double fullHeight = list.prefHeight(-1);
        double targetHeight = open ? fullHeight : 0;
        double targetOpacity = open ? 1 : 0;
        double targetTurn = open ? 180 : 0;
        if (!animate) {
            finish(targetHeight, targetOpacity, targetTurn);
            return;
        }
        list.setMinHeight(0);
        list.setMaxHeight(list.getHeight());
        running = new Timeline(new KeyFrame(SLIDE,
            new KeyValue(list.maxHeightProperty(), targetHeight, Interpolator.EASE_BOTH),
            new KeyValue(list.opacityProperty(), targetOpacity, Interpolator.EASE_BOTH),
            new KeyValue(chevron.rotateProperty(), targetTurn, Interpolator.EASE_BOTH)));
        running.setOnFinished(event -> finish(targetHeight, targetOpacity, targetTurn));
        running.play();
    }

    private void finish(double height, double opacity, double turn) {
        // Open: its natural size and no shrinking; closed: no height at all.
        list.setMinHeight(open ? Region.USE_COMPUTED_SIZE : 0);
        list.setMaxHeight(open ? Region.USE_COMPUTED_SIZE : height);
        list.setOpacity(opacity);
        list.setVisible(open);
        chevron.setRotate(turn);
    }
}
