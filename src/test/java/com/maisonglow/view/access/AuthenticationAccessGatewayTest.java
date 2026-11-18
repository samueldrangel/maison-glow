package com.maisonglow.view.access;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Access gateway over the real account service")
class AuthenticationAccessGatewayTest {

    private static final String PASSWORD = "clave-segura-1";

    @TempDir
    Path data;

    private AppContext context;
    private AccessGateway access;

    @BeforeEach
    void open() {
        context = new AppContext(data, Clock.systemDefaultZone(), AiConfig.simulated());
        access = context.access();
    }

    private static RegistrationData ana(String username) {
        return new RegistrationData("Ana García", "1001", "3001112233", "ana@correo.com", username, PASSWORD, PASSWORD);
    }

    @Test
    @DisplayName("the application starts with no session and no administrator")
    void startsEmpty() {
        assertThat(access.current()).isEmpty();
        assertThatThrownBy(() -> access.login("admin", "admin-admin-1")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("an administrator created by the controlled setup signs in with the role and the name of the account")
    void administratorSignsIn() throws Exception {
        context.auth().createFirstAdmin("jefa", "clave-de-prueba-1".toCharArray());

        AccessSession session = access.login("JEFA", "clave-de-prueba-1");

        assertThat(session.isAdministrator()).isTrue();
        assertThat(session.displayName()).isEqualTo("jefa");
        assertThat(access.current()).contains(session);
        access.logout();
        assertThat(access.current()).isEmpty();
    }

    @Test
    @DisplayName("a sign-up creates a customer with a new record, opens its session and shows the customer's name")
    void registerCreatesACustomer() throws Exception {
        AccessSession session = access.register(ana("ana"));

        assertThat(session.role()).isEqualTo(AccessRole.CUSTOMER);
        assertThat(session.displayName()).isEqualTo("Ana García");
        assertThat(context.portal().profile().getDocument()).isEqualTo("1001");
        access.logout();
        assertThat(access.login("ana", PASSWORD).displayName()).isEqualTo("Ana García");
    }

    @Test
    @DisplayName("what the forms get wrong comes back as the exceptions the screens place under their field")
    void rejectsWhatIsWrong() throws Exception {
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "3001112233", "a@b.co", "ana", "corta", "corta")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("contraseña");
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "3001112233", "a@b.co", "ana", PASSWORD, "otra-clave-12")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("confirmación");
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "3001112233", "sin-arroba", "ana", PASSWORD, PASSWORD)))
            .isInstanceOf(ValidationException.class).hasMessageContaining("correo");
        assertThatThrownBy(() -> access.login("", "x")).isInstanceOf(ValidationException.class).hasMessageContaining("usuario");
        assertThat(access.current()).isEmpty();

        access.register(ana("ana"));
        access.logout();
        assertThatThrownBy(() -> access.register(new RegistrationData("Otra", "2002", "3001112233", "o@b.co", "ANA", PASSWORD, PASSWORD)))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("usuario");
        assertThatThrownBy(() -> access.login("ana", "equivocada-123")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("changing the password needs the current one; the service then ends the session and only the new one works")
    void changePassword() throws Exception {
        access.register(ana("ana"));

        assertThatThrownBy(() -> access.changePassword("mala-mala-mala", "nueva-clave-12", "nueva-clave-12"))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("actual");
        assertThatThrownBy(() -> access.changePassword(PASSWORD, "nueva-clave-12", "distinta-12345"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("confirmación");
        access.changePassword(PASSWORD, "nueva-clave-12", "nueva-clave-12");

        assertThat(access.current()).isEmpty();
        assertThatThrownBy(() -> access.login("ana", PASSWORD)).isInstanceOf(BusinessRuleException.class);
        assertThat(access.login("ana", "nueva-clave-12")).isNotNull();
    }

    @Test
    @DisplayName("changing the password with no session says the session ended, so the window goes back to the sign-in")
    void changePasswordWithoutSession() {
        assertThatThrownBy(() -> access.changePassword(PASSWORD, "nueva-clave-12", "nueva-clave-12"))
            .isInstanceOf(SessionExpiredException.class).hasMessage(SessionExpiredException.MESSAGE);
    }

    @Test
    @DisplayName("a deactivated account loses its session and cannot sign in again")
    void deactivatedAccount() throws Exception {
        context.auth().createFirstAdmin("jefa", "clave-de-prueba-1".toCharArray());
        access.register(ana("ana"));
        access.logout();
        access.login("jefa", "clave-de-prueba-1");
        int anaId = accountIdOf("ana");

        context.auth().setActive(anaId, false);
        access.logout();

        assertThatThrownBy(() -> access.login("ana", PASSWORD)).isInstanceOf(BusinessRuleException.class);
    }

    private int accountIdOf(String username) throws Exception {
        return context.database().transaction(connection -> {
            try (var statement = connection.prepareStatement("SELECT id FROM app_user WHERE username = ?")) {
                statement.setString(1, username);
                try (var rows = statement.executeQuery()) {
                    rows.next();
                    return rows.getInt(1);
                }
            }
        });
    }
}
