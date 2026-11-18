package com.maisonglow.view.controller;

import com.maisonglow.AppContext;
import com.maisonglow.TestAccounts;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.model.UserAccount;
import com.maisonglow.service.ContactData;
import com.maisonglow.view.AlertPresenter;
import com.maisonglow.view.FxTest;
import com.maisonglow.view.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Accounts view tests")
class AccountsViewTest {

    private static final char[] PASSWORD = "clave-segura-123".toCharArray();

    private static boolean toolkit;

    @TempDir
    Path data;

    private AppContext context;
    private final List<String> dialogs = new ArrayList<>();
    private final List<Runnable> held = new ArrayList<>();

    @BeforeAll
    static void startToolkit() {
        toolkit = FxTest.start();
    }

    @BeforeEach
    void prepare() throws Exception {
        assumeTrue(toolkit, "JavaFX toolkit needs a display");
        AppContext.seed(data);
        context = TestAccounts.open(data);
        context.auth().registerCustomer(new ContactData("1001", "Ana García", "3001112233", "ana@correo.com"), "ana", PASSWORD);
        context.auth().registerCustomer(new ContactData("1002", "Laura Pérez", "3002223344", "laura@correo.com"), "laura", PASSWORD);
    }

    private Parent open(boolean confirm) throws Exception {
        return FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AccountsController> loaded = ViewLoader.loadWithController(
                AccountsViewTest.class.getResource("/com/maisonglow/view/AccountsView.fxml"), context);
            loaded.controller().useExecutor(Runnable::run);
            loaded.controller().useAlerts(new AlertPresenter(() -> null, alert -> {
                dialogs.add(alert.getContentText());
                return alert.getButtonTypes().stream().filter(button -> confirm && "Confirmar".equals(button.getText())).findFirst();
            }));
            new Scene(loaded.root(), 1000, 660);
            loaded.root().applyCss();
            loaded.root().layout();
            return loaded.root();
        });
    }

    @SuppressWarnings("unchecked")
    private static TableView<Object> table(Parent root) {
        return (TableView<Object>) root.lookup("#accountsTable");
    }

    private static void settle() throws Exception {
        for (int round = 0; round < 3; round++) {
            FxTest.onFxThread(() -> null);
            Thread.sleep(60);
        }
    }

    /** What the user name column shows, row by row (call it on the JavaFX thread). */
    private static List<String> usersNow(Parent root) {
        root.applyCss();
        root.layout();
        TableView<Object> table = table(root);
        @SuppressWarnings({"unchecked", "rawtypes"})
        javafx.scene.control.TableColumn<Object, Object> column = (javafx.scene.control.TableColumn) table.getColumns().getFirst();
        return table.getItems().stream().map(row -> String.valueOf(column.getCellData(row))).toList();
    }

    private static List<String> users(Parent root) throws Exception {
        settle();
        return FxTest.onFxThread(() -> usersNow(root));
    }

    private static void chip(Parent root, String text) throws Exception {
        FxTest.onFxThread(() -> {
            root.lookupAll(".filter-chip").stream().map(ToggleButton.class::cast)
                .filter(chip -> text.equals(chip.getText())).findFirst().orElseThrow().fire();
            return null;
        });
    }

    /** The action button of the row of a user. */
    private static Button actionOf(Parent root, String username) throws Exception {
        settle();
        return FxTest.onFxThread(() -> {
            int index = usersNow(root).indexOf(username);
            for (javafx.scene.Node node : root.lookupAll(".table-row-cell")) {
                javafx.scene.control.TableRow<?> row = (javafx.scene.control.TableRow<?>) node;
                if (row.getIndex() == index) {
                    return row.lookupAll(".button").stream().filter(Button.class::isInstance).map(Button.class::cast)
                        .findFirst().orElseThrow();
                }
            }
            throw new AssertionError("No row for " + username);
        });
    }

    private static void press(Button button) throws Exception {
        FxTest.onFxThread(() -> {
            button.fire();
            return null;
        });
        settle();
    }

    private static boolean isActive(AppContext context, String username) throws Exception {
        return context.auth().listAccounts().stream().filter(account -> account.username().equals(username))
            .map(UserAccount::active).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("lists every account with its role and the customer it belongs to, and shows nothing to sign in with")
    void listsTheAccounts() throws Exception {
        Parent root = open(true);

        assertThat(users(root)).containsExactlyInAnyOrder("fixture-admin", "ana", "laura");
        assertThat(FxTest.onFxThread(() -> table(root).getColumns().stream().map(column -> column.getText()).toList()))
            .containsExactly("Usuario", "Rol", "Cliente", "Estado", "Acción");
        assertThat(FxTest.onFxThread(() -> root.lookupAll(".table-cell").stream().filter(TableCell.class::isInstance)
            .map(cell -> ((TableCell<?, ?>) cell).getText()).filter(java.util.Objects::nonNull).toList()))
            .contains("Ana García", "Laura Pérez", "Administración", "Cliente")
            .noneMatch(text -> text.length() > 40 || text.contains("clave"));
    }

    @Test
    @DisplayName("the chips split active and deactivated accounts, and the search finds by username or customer")
    void filtersAndSearches() throws Exception {
        context.auth().setActive(context.auth().listAccounts().stream().filter(a -> a.username().equals("laura")).findFirst().orElseThrow().id(), false);
        Parent root = open(true);

        chip(root, "Desactivadas");
        assertThat(users(root)).containsExactly("laura");
        chip(root, "Activas");
        assertThat(users(root)).containsExactlyInAnyOrder("fixture-admin", "ana");
        chip(root, "Todas");
        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("garcia"); return null; });
        assertThat(users(root)).containsExactly("ana");
        FxTest.onFxThread(() -> { ((TextField) root.lookup("#searchField")).setText("zzz"); return null; });
        assertThat(users(root)).isEmpty();
        assertThat(FxTest.onFxThread(() -> ((Label) root.lookup("#emptyLabel")).getText())).contains("coincide");
    }

    @Test
    @DisplayName("the account in use cannot be turned off from here, the others can")
    void ownAccountIsProtected() throws Exception {
        Parent root = open(true);

        assertThat(actionOf(root, "fixture-admin").isDisabled()).isTrue();
        assertThat(actionOf(root, "ana").isDisabled()).isFalse();
    }

    @Test
    @DisplayName("deactivating asks first, turns the account off and the customer can no longer sign in")
    void deactivates() throws Exception {
        Parent root = open(true);

        press(actionOf(root, "ana"));

        assertThat(dialogs).anyMatch(text -> text.contains("Se cerrarán sus sesiones"));
        assertThat(isActive(context, "ana")).isFalse();
        assertThat(actionOf(root, "ana").getText()).isEqualTo("Activar");
        assertThatThrownBy(() -> context.auth().login("ana", PASSWORD)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("answering no to the confirmation changes nothing")
    void declinedDeactivationChangesNothing() throws Exception {
        Parent root = open(false);

        press(actionOf(root, "ana"));

        assertThat(isActive(context, "ana")).isTrue();
    }

    @Test
    @DisplayName("activating an account turns it on again with no question and the customer can sign in")
    void activates() throws Exception {
        context.auth().setActive(context.auth().listAccounts().stream().filter(a -> a.username().equals("ana")).findFirst().orElseThrow().id(), false);
        Parent root = open(true);
        dialogs.clear();

        press(actionOf(root, "ana"));

        assertThat(dialogs).isEmpty();
        assertThat(isActive(context, "ana")).isTrue();
        assertThat(context.auth().login("ana", PASSWORD).username()).isEqualTo("ana");
    }

    @Test
    @DisplayName("with the session ended the service refuses and the administrator is told, with nothing changed")
    void serviceErrorIsShown() throws Exception {
        Parent root = open(true);
        Button ana = actionOf(root, "ana");
        context.auth().logout();

        press(ana);

        assertThat(dialogs).anyMatch(text -> text.contains("Inicia sesión"));
    }

    @Test
    @DisplayName("an answer that arrives after the screen was discarded is ignored and the list is dropped")
    void lateAnswerIsIgnored() throws Exception {
        var loaded = FxTest.onFxThread(() -> {
            ViewLoader.Loaded<AccountsController> opened = ViewLoader.loadWithController(
                AccountsViewTest.class.getResource("/com/maisonglow/view/AccountsView.fxml"), context);
            opened.controller().useExecutor(held::add);
            new Scene(opened.root(), 1000, 660);
            opened.root().applyCss();
            opened.root().layout();
            return opened;
        });
        FxTest.onFxThread(() -> { loaded.controller().dispose(); return null; });
        held.forEach(Runnable::run);
        settle();

        assertThat(FxTest.onFxThread(() -> table(loaded.root()).getItems())).isEmpty();
    }
}
