package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.view.NavGroup;
import com.maisonglow.view.Navigator;
import com.maisonglow.view.View;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller of {@code MainView.fxml}: the side menu and the central area. It
 * only chooses which screen to show; it does not call any service.
 */
public final class MainController {

    private final AppContext context;
    private final List<NavGroup> groups = new ArrayList<>();

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
        refreshBadges();
        navigator.show(View.HOME);
    }

    /** Sub-entries that follow the current screen. */
    private void setUpGroups() {
        sidebar.lookupAll(".nav-group-box").forEach(box -> groups.add(new NavGroup((VBox) box)));
        navigation.selectedToggleProperty().addListener((observable, previous, selected) -> showCurrentGroup(true));
        showCurrentGroup(false);
    }

    /** Opens the group that holds the current screen and closes the others. */
    private void showCurrentGroup(boolean animate) {
        Toggle selected = navigation.getSelectedToggle();
        groups.forEach(group -> group.setOpen(group.contains(selected), animate));
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
    }

    @FunctionalInterface
    private interface Count {
        int get() throws ValidationException, DataAccessException;
    }
}
