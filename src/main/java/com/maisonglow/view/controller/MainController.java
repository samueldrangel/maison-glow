package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.view.Animations;
import com.maisonglow.view.NavGroup;
import com.maisonglow.view.Navigator;
import com.maisonglow.view.SidebarLogo;
import com.maisonglow.view.View;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller of {@code MainView.fxml}: the side menu and the central area. It
 * only chooses which screen to show; it does not call any service.
 */
public final class MainController {

    /** Widths of the side menu; they match {@code .sidebar} in styles.css. */
    private static final double COLLAPSED_WIDTH = 76;
    private static final double EXPANDED_WIDTH = 240;
    private static final Duration EXPAND_MOTION = Duration.millis(280);
    private static final Duration COLLAPSE_MOTION = Duration.millis(240);
    /** The bar waits a moment before closing, so leaving the edge for an instant does not make it blink. */
    private static final Duration COLLAPSE_DELAY = Duration.millis(140);
    private static final Interpolator EXPAND_CURVE = Interpolator.SPLINE(0.33, 1, 0.68, 1);
    private static final Interpolator COLLAPSE_CURVE = Interpolator.SPLINE(0.65, 0, 0.35, 1);

    private final AppContext context;
    private final List<NavGroup> groups = new ArrayList<>();
    private final DoubleProperty sidebarWidth = new SimpleDoubleProperty(COLLAPSED_WIDTH);
    private Timeline sidebarMotion;
    private PauseTransition collapseDelay;
    private SidebarLogo logo;

    @FXML
    private StackPane content;

    @FXML
    private VBox sidebar;

    @FXML
    private ToggleGroup navigation;

    @FXML
    private Label appointmentsBadge;

    @FXML
    private Label productsBadge;

    private Navigator navigator;

    /**
     * Creates the controller (called by the {@code ViewLoader}).
     *
     * @param context services shared by every view
     */
    public MainController(AppContext context) {
        this.context = Objects.requireNonNull(context, "context");
    }

    @FXML
    private void initialize() {
        navigator = new Navigator(content, context);
        // A selected menu entry cannot be unselected by clicking it again.
        navigation.selectedToggleProperty().addListener((observable, previous, selected) -> {
            if (selected == null && previous != null) {
                previous.setSelected(true);
            }
        });
        setUpGroups();
        setUpMotion();
        refreshBadges();
        navigator.show(View.HOME);
    }

    /** Sub-entries that follow the current screen. */
    private void setUpGroups() {
        sidebar.lookupAll(".nav-group-box").forEach(box -> groups.add(new NavGroup((VBox) box)));
        navigation.selectedToggleProperty().addListener((observable, previous, selected) -> showCurrentGroup(true));
        showCurrentGroup(false);
    }

    /** A sidebar that slides open and icons that react to the cursor. */
    private void setUpMotion() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebar.widthProperty());
        clip.heightProperty().bind(sidebar.heightProperty());
        sidebar.setClip(clip);
        logo = new SidebarLogo((Region) sidebar.lookup(".logo-slot"));
        // The CSS widths are for Scene Builder; from here on the width is moved from code.
        sidebar.getStyleClass().remove("css-motion");
        applyWidth(COLLAPSED_WIDTH);
        sidebarWidth.addListener((observable, was, width) -> applyWidth(width.doubleValue()));
        sidebar.hoverProperty().addListener((observable, was, hovered) -> onSidebarHover(hovered));

        sidebar.lookupAll(".nav-button").stream().filter(Labeled.class::isInstance)
            .forEach(button -> Animations.nudgeOnHover((Labeled) button));
    }

    /** Opens the group that holds the current screen and closes the others. */
    private void showCurrentGroup(boolean animate) {
        Toggle selected = navigation.getSelectedToggle();
        groups.forEach(group -> group.setOpen(group.contains(selected), animate));
    }

    /** Opens at once when the cursor arrives; closes after a short delay when it leaves. */
    private void onSidebarHover(boolean hovered) {
        if (collapseDelay != null) {
            collapseDelay.stop();
        }
        if (hovered) {
            moveSidebar(true);
            return;
        }
        collapseDelay = new PauseTransition(COLLAPSE_DELAY);
        collapseDelay.setOnFinished(event -> moveSidebar(false));
        collapseDelay.play();
    }

    /** Grows or shrinks the sidebar; the "expanded" look stays until it has finished shrinking. */
    private void moveSidebar(boolean expand) {
        if (sidebarMotion != null) {
            sidebarMotion.stop();
        }
        if (expand && !sidebar.getStyleClass().contains("expanded")) {
            sidebar.getStyleClass().add("expanded");
        }
        sidebarWidth.set(sidebar.getWidth());
        sidebarMotion = new Timeline(new KeyFrame(expand ? EXPAND_MOTION : COLLAPSE_MOTION,
            new KeyValue(sidebarWidth, expand ? EXPANDED_WIDTH : COLLAPSED_WIDTH, expand ? EXPAND_CURVE : COLLAPSE_CURVE)));
        sidebarMotion.setOnFinished(event -> {
            if (!expand) {
                sidebar.getStyleClass().remove("expanded");
            }
        });
        sidebarMotion.play();
    }

    /** Sets the width of the bar and draws the logo for that width. */
    private void applyWidth(double width) {
        sidebar.setMinWidth(width);
        sidebar.setPrefWidth(width);
        sidebar.setMaxWidth(width);
        logo.update((width - COLLAPSED_WIDTH) / (EXPANDED_WIDTH - COLLAPSED_WIDTH));
    }

    @FXML
    private void onNavigate(ActionEvent event) {
        Node source = (Node) event.getSource();
        navigator.show(View.valueOf((String) source.getUserData()));
    }

    /** The header of a group (Citas) leads to its first sub-entry, unless one of them is already open. */
    @FXML
    private void onOpenGroup(ActionEvent event) {
        Node header = (Node) event.getSource();
        groups.stream().filter(group -> group.owns(header) && !group.contains(navigation.getSelectedToggle()))
            .findFirst().ifPresent(NavGroup::openFirst);
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    /** Shows today's appointments and the low-stock products next to their menu entries. */
    private void refreshBadges() {
        showCount(appointmentsBadge, () -> context.appointments().listByDate(LocalDate.now(context.clock())).size());
        showCount(productsBadge, () -> context.products().listLowStock().size());
    }

    private static void showCount(Label badge, Count count) {
        int value;
        try {
            value = count.get();
        } catch (ValidationException | DataAccessException e) {
            value = 0;
        }
        badge.setText(String.valueOf(value));
        badge.setVisible(value > 0);
        badge.setManaged(value > 0);
        if (value > 0) {
            Animations.pop(badge);
        }
    }

    @FunctionalInterface
    private interface Count {
        int get() throws ValidationException, DataAccessException;
    }
}
