package com.maisonglow.view.access;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("In-memory access gateway tests")
class InMemoryAccessGatewayTest {

    private InMemoryAccessGateway access;

    @BeforeEach
    void create() {
        access = new InMemoryAccessGateway();
    }

    private static RegistrationData ana() {
        return new RegistrationData("Ana García", "1001", "3001112233", "ana@correo.com", "Ana", "clave-segura-1", "clave-segura-1");
    }

    @Test
    @DisplayName("a sign-up creates a customer, opens its session and never an administrator")
    void registerCreatesACustomer() throws Exception {
        AccessSession session = access.register(ana());

        assertThat(session.role()).isEqualTo(AccessRole.CUSTOMER);
        assertThat(session.isAdministrator()).isFalse();
        assertThat(session.displayName()).isEqualTo("Ana García");
        assertThat(access.current()).contains(session);
    }

    @Test
    @DisplayName("a sign-up with a missing or malformed field says which one")
    void registerValidatesEveryField() {
        assertThatThrownBy(() -> access.register(new RegistrationData(" ", "1", "3001112233", "a@b.co", "u", "clave-segura-1", "clave-segura-1")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("nombre");
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "12", "a@b.co", "u", "clave-segura-1", "clave-segura-1")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("teléfono");
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "3001112233", "sin-arroba", "u", "clave-segura-1", "clave-segura-1")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("correo");
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "3001112233", "a@b.co", "u", "corta", "corta")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("contraseña").hasMessageContaining("12");
        assertThatThrownBy(() -> access.register(new RegistrationData("Ana", "1", "3001112233", "a@b.co", "u", "clave-segura-1", "otra-clave-12")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("confirmación");
        assertThat(access.current()).isEmpty();
    }

    @Test
    @DisplayName("a username or a document already in use is refused, whatever the case of the username")
    void registerRefusesDuplicates() throws Exception {
        access.register(ana());
        access.logout();

        assertThatThrownBy(() -> access.register(new RegistrationData("Otra", "2002", "3001112233", "o@b.co", "ANA", "clave-segura-1", "clave-segura-1")))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("usuario");
        assertThatThrownBy(() -> access.register(new RegistrationData("Otra", "1001", "3001112233", "o@b.co", "otra", "clave-segura-1", "clave-segura-1")))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("documento");
    }

    @Test
    @DisplayName("signing in works with the right password; a wrong user and a wrong password get the same message")
    void loginChecksTheCredentials() throws Exception {
        access.register(ana());
        access.logout();

        assertThat(access.login("ana", "clave-segura-1").username()).isEqualTo("ana");
        access.logout();
        assertThatThrownBy(() -> access.login("ana", "equivocada")).isInstanceOf(BusinessRuleException.class)
            .hasMessage("Usuario o contraseña incorrectos.");
        assertThatThrownBy(() -> access.login("nadie", "clave-segura-1")).isInstanceOf(BusinessRuleException.class)
            .hasMessage("Usuario o contraseña incorrectos.");
        assertThatThrownBy(() -> access.login("", "x")).isInstanceOf(ValidationException.class).hasMessageContaining("usuario");
        assertThat(access.current()).isEmpty();
    }

    @Test
    @DisplayName("an administrator exists only if something creates it; nobody starts with one")
    void noAdministratorByDefault() throws Exception {
        assertThatThrownBy(() -> access.login("admin", "admin")).isInstanceOf(BusinessRuleException.class);

        access.addAdministrator("jefa", "La jefa", "clave-de-prueba");

        assertThat(access.login("jefa", "clave-de-prueba").isAdministrator()).isTrue();
    }

    @Test
    @DisplayName("changing the password needs the current one, a new valid one and a session")
    void changePassword() throws Exception {
        access.register(ana());

        assertThatThrownBy(() -> access.changePassword("mala", "nueva-clave-1", "nueva-clave-1"))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("actual");
        assertThatThrownBy(() -> access.changePassword("clave-segura-1", "nueva-clave-1", "distinta-1"))
            .isInstanceOf(ValidationException.class).hasMessageContaining("confirmación");
        access.changePassword("clave-segura-1", "nueva-clave-1", "nueva-clave-1");
        access.logout();

        assertThatThrownBy(() -> access.login("ana", "clave-segura-1")).isInstanceOf(BusinessRuleException.class);
        assertThat(access.login("ana", "nueva-clave-1")).isNotNull();
        access.logout();
        assertThatThrownBy(() -> access.changePassword("nueva-clave-1", "otra-clave-22", "otra-clave-22"))
            .isInstanceOf(BusinessRuleException.class).hasMessageContaining("sesión");
    }

    @Test
    @DisplayName("logging out closes the session and the data object does not print the password")
    void logoutClosesTheSession() throws Exception {
        access.register(ana());

        access.logout();
        access.logout();

        assertThat(access.current()).isEmpty();
        assertThat(ana().toString()).doesNotContain("clave-segura-1");
    }
}
