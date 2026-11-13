package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.view.Animations;
import com.maisonglow.view.NavGroup;
import com.maisonglow.view.Navigator;
import com.maisonglow.view.View;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Controller of {@code MainView.fxml}: the side menu and the central area. It
 * only chooses which screen to show; it does not call any service.
 */
public final class MainController {

    /** Widths of the side menu; they match {@code .sidebar} in styles.css. */
    private static final double COLLAPSED_WIDTH = 76;
    private static final double EXPANDED_WIDTH = 240;
    private static final Duration SIDEBAR_MOTION = Duration.millis(180);

    private final AppContext context;
    private final List<NavGroup> groups = new ArrayList<>();
    private final DoubleProperty sidebarWidth = new SimpleDoubleProperty(COLLAPSED_WIDTH);
    private Timeline sidebarMotion;

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
        sidebarWidth.addListener((observable, was, width) -> sidebar.setStyle(widthStyle(width.doubleValue())));
        sidebar.hoverProperty().addListener((observable, was, hovered) -> moveSidebar(hovered));

        sidebar.lookupAll(".nav-button").stream().filter(Labeled.class::isInstance)
            .forEach(button -> Animations.nudgeOnHover((Labeled) button));
    }

    /** Opens the group that holds the current screen and closes the others. */
    private void showCurrentGroup(boolean animate) {
        Toggle selected = navigation.getSelectedToggle();
        groups.forEach(group -> group.setOpen(group.contains(selected), animate));
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
        // Set it now: the property may not change (same value), and the :hover rule would jump first.
        sidebar.setStyle(widthStyle(sidebarWidth.get()));
        sidebarMotion = new Timeline(new KeyFrame(SIDEBAR_MOTION,
            new KeyValue(sidebarWidth, expand ? EXPANDED_WIDTH : COLLAPSED_WIDTH, Interpolator.EASE_BOTH)));
        sidebarMotion.setOnFinished(event -> {
            // The CSS already describes the final state, so it takes over again.
            sidebar.setStyle("");
            if (!expand) {
                sidebar.getStyleClass().remove("expanded");
            }
        });
        sidebarMotion.play();
    }

    /**
     * The width goes in an inline style because that is the only thing that wins over styles.css (a
     * value set from code would lose against the {@code :hover} rule and the bar would flicker).
     */
    private static String widthStyle(double width) {
        return String.format(Locale.ROOT, "-fx-min-width: %1$.1f; -fx-pref-width: %1$.1f; -fx-max-width: %1$.1f;", width);
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
