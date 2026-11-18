package com.maisonglow.integration;

import com.maisonglow.AppContext;
import com.maisonglow.ai.AiConfig;
import com.maisonglow.dao.AccountDao;
import com.maisonglow.dao.AccountDaoSqlite;
import com.maisonglow.dao.CustomerDaoSqlite;
import com.maisonglow.dao.SqliteTransactions;
import com.maisonglow.exception.AuthorizationException;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.UserAccount;
import com.maisonglow.model.UserRole;
import com.maisonglow.service.AuthenticationService;
import com.maisonglow.service.ContactData;
import com.maisonglow.service.auth.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises real SQLite transactions, persisted credentials and multiple desktop contexts. */
class AuthenticationIntegrationTest {
    @TempDir Path folder;
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-17T15:00:00Z"), ZoneOffset.UTC);
    private static char[] password() { return "fixture-only-password".toCharArray(); }
    private AppContext open() { return new AppContext(folder, CLOCK, AiConfig.simulated()); }
    private com.maisonglow.service.CustomerService customers(AppContext app) { return new com.maisonglow.service.CustomerService(new CustomerDaoSqlite(app.database())); }
    private ContactData contact(String document) { return new ContactData(document, "Cliente de prueba", "3001234567", "test@example.invalid"); }
    private UserAccount customer(AppContext app, String name) throws Exception {
        return app.auth().registerCustomer(contact(name), name, password());
    }
    private UserAccount admin(AppContext app) throws Exception {
        var account = app.auth().createFirstAdmin("admin", password());
        app.auth().login("admin", password());
        return account;
    }

    @Test void registrationIsAtomicAndPersistsWithoutAutomaticallyLoggingIn() throws Exception {
        var app = open();
        var account = customer(app, "alice");
        assertThat(account.role()).isEqualTo(UserRole.CUSTOMER);
        assertThat(customers(app).findById(account.customerId()).getDocument()).isEqualTo("alice");
        assertThat(app.auth().currentSession()).isEmpty();
        var stored = new AccountDaoSqlite(app.database()).findById(account.id()).orElseThrow();
        assertThat(stored.passwordHash()).startsWith("pbkdf2-sha256$600000$").doesNotContain("fixture-only-password");
        assertThat(stored.toString()).doesNotContain(stored.passwordHash());
        assertThat(open().auth().login("  ALICE  ", password())).isEqualTo(account);
    }

    @Test void registrationCannotClaimExistingCustomersOrCreateDuplicateLogins() throws Exception {
        var app = open();
        customer(app, "alice");
        assertThatThrownBy(() -> app.auth().registerCustomer(contact("new-doc"), " ALICE ", password()))
            .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.auth().registerCustomer(contact("alice"), "another", password()))
            .isInstanceOf(BusinessRuleException.class);
        assertThat(customers(app).listAll()).hasSize(1);
        assertThatThrownBy(() -> app.auth().linkCustomerAccount(1, "another", password()))
            .isInstanceOf(AuthorizationException.class);
    }

    @Test void accountFailureRollsBackTheCustomerInsert() throws Exception {
        var app = open();
        var accounts = spy(new AccountDaoSqlite(app.database()));
        doThrow(new DataAccessException("Fallo de escritura controlado"))
            .when(accounts).create(anyString(), anyString(), eq(UserRole.CUSTOMER), anyInt());
        var auth = new AuthenticationService(accounts, new CustomerDaoSqlite(app.database()), customers(app),
            new SqliteTransactions(app.database()), new PasswordHasher(), CLOCK);
        assertThatThrownBy(() -> auth.registerCustomer(contact("new-doc"), "alice", password()))
            .isInstanceOf(DataAccessException.class);
        assertThat(customers(app).listAll()).isEmpty();
        assertThat(accounts.findByUsername("alice")).isEmpty();
    }

    @Test void anonymousAndCustomersCannotUseAdministratorOrOtherCustomerPermissions() throws Exception {
        var app = open();
        assertThatThrownBy(() -> app.auth().requireAdmin()).isInstanceOf(AuthorizationException.class);
        var alice = customer(app, "alice");
        var bob = customer(app, "bobby");
        app.auth().login("alice", password());
        assertThat(app.auth().currentCustomerId()).isEqualTo(alice.customerId());
        app.auth().requireCustomerAccess(alice.customerId());
        assertThatThrownBy(() -> app.auth().requireCustomerAccess(bob.customerId())).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.auth().requireCustomerAccess(0)).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.auth().requireAdmin()).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.auth().setActive(bob.id(), false)).isInstanceOf(AuthorizationException.class);
        app.auth().logout();
        assertThatThrownBy(() -> app.auth().currentCustomerId()).isInstanceOf(AuthorizationException.class);
    }

    @Test void firstAdministratorIsExplicitAndCannotBeRecreatedOrBeDeactivatedAsLastAdmin() throws Exception {
        var app = open();
        var admin = admin(app);
        assertThat(admin.role()).isEqualTo(UserRole.ADMIN);
        assertThat(admin.customerId()).isNull();
        assertThatThrownBy(() -> app.auth().createFirstAdmin("second-admin", password())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.auth().setActive(admin.id(), false)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> app.auth().currentCustomerId()).isInstanceOf(AuthorizationException.class);
        app.auth().requireCustomerAccess(123);
        assertThat(customers(app).listAll()).isEmpty();
    }

    @Test void administratorCanLinkOnlyAnExistingUnclaimedCustomer() throws Exception {
        var app = open();
        admin(app);
        int id = customers(app).register(contact("legacy-document")).getId();
        var account = app.auth().linkCustomerAccount(id, "legacy-user", password());
        assertThat(account.customerId()).isEqualTo(id);
        assertThatThrownBy(() -> app.auth().linkCustomerAccount(id, "other-user", password())).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> app.auth().linkCustomerAccount(999, "not-found", password())).isInstanceOf(BusinessRuleException.class);
        app.auth().login("legacy-user", password());
        assertThat(app.auth().currentCustomerId()).isEqualTo(id);
    }

    @Test void failedLoginClearsThePreviousSessionAndDoesNotRevealWhichCredentialWasWrong() throws Exception {
        var app = open(); customer(app, "alice");
        app.auth().login("alice", password());
        assertThatThrownBy(() -> app.auth().login("alice", "incorrect-password".toCharArray()))
            .isInstanceOf(AuthorizationException.class).hasMessageContaining("Usuario o contraseña incorrectos");
        assertThat(app.auth().currentSession()).isEmpty();
        assertThatThrownBy(() -> app.auth().login("missing", password()))
            .isInstanceOf(AuthorizationException.class).hasMessageContaining("Usuario o contraseña incorrectos");
        app.auth().login("alice", password());
        assertThatThrownBy(() -> app.auth().login("?", password())).isInstanceOf(ValidationException.class);
        assertThat(app.auth().currentSession()).isEmpty();
    }

    @Test void passwordChangeInvalidatesAllOpenContextsAndOldPassword() throws Exception {
        var app = open(); customer(app, "alice");
        var second = open();
        app.auth().login("alice", password()); second.auth().login("alice", password());
        assertThatThrownBy(() -> app.auth().changePassword("wrong-password".toCharArray(), "new-fixture-password".toCharArray()))
            .isInstanceOf(AuthorizationException.class);
        assertThat(app.auth().currentSession()).isPresent();
        app.auth().changePassword(password(), "new-fixture-password".toCharArray());
        assertThat(app.auth().currentSession()).isEmpty();
        assertThat(second.auth().currentSession()).isEmpty();
        assertThatThrownBy(() -> app.auth().login("alice", password())).isInstanceOf(AuthorizationException.class);
        app.auth().login("alice", "new-fixture-password".toCharArray());
    }

    @Test void deactivationAndReactivationNeverReviveOldSessions() throws Exception {
        var app = open(); admin(app); var account = customer(app, "alice");
        var second = open(); second.auth().login("alice", password());
        app.auth().setActive(account.id(), false);
        app.auth().setActive(account.id(), true);
        assertThat(second.auth().currentSession()).isEmpty();
        second.auth().login("alice", password());
        app.auth().setActive(account.id(), false);
        assertThat(second.auth().currentSession()).isEmpty();
        assertThatThrownBy(() -> second.auth().login("alice", password())).isInstanceOf(AuthorizationException.class);
        assertThatThrownBy(() -> app.auth().setActive(999, false)).isInstanceOf(BusinessRuleException.class);
        app.auth().setActive(account.id(), false); // idempotent, no accidental reactivation
    }

    @Test void sessionExpiresAtEightHoursWithoutSleeping() throws Exception {
        var app = open(); customer(app, "alice");
        var time = new MutableClock();
        var auth = new AuthenticationService(new AccountDaoSqlite(app.database()), new CustomerDaoSqlite(app.database()),
            customers(app), new SqliteTransactions(app.database()), new PasswordHasher(), time);
        auth.login("alice", password());
        time.now = time.now.plus(Duration.ofHours(8));
        assertThat(auth.currentSession()).isEmpty();
    }

    @Test void validationPrecedesWritesAndDatabaseEnforcesNormalizedUniqueness() throws Exception {
        var app = open();
        assertThatThrownBy(() -> app.auth().registerCustomer(null, "alice", password())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> app.auth().registerCustomer(contact("one"), null, password())).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> app.auth().registerCustomer(contact("one"), "alice", "short".toCharArray())).isInstanceOf(ValidationException.class);
        assertThat(customers(app).listAll()).isEmpty();
        customer(app, "alice");
        assertThatThrownBy(() -> app.database().transaction(c -> {
            try (var s = c.createStatement()) {
                s.executeUpdate("INSERT INTO app_user(username,password_hash,role) VALUES(' ALICE ','unsupported','ADMIN')");
            }
            return null;
        })).isInstanceOf(DataAccessException.class);
    }

    @Test void upgradePreservesLegacyAccountsAndBusinessDataAndCanBeRepeated() throws Exception {
        var app = open(); var account = customer(app, "alice");
        new com.maisonglow.service.SettingsService(new com.maisonglow.dao.SettingDaoSqlite(app.database())).update("business_name", "Salón conservado");
        app.database().transaction(c -> {
            try (var s = c.createStatement()) {
                s.executeUpdate("DELETE FROM schema_migration");
                s.execute("DROP INDEX idx_app_user_normalized_username");
                s.execute("ALTER TABLE app_user DROP COLUMN auth_version");
            }
            return null;
        });
        Path backup = folder.resolve("before-upgrade.db");
        Files.copy(app.database().file(), backup);
        var upgraded = open();
        assertThat(upgraded.auth().login("alice", password())).isEqualTo(account);
        assertThat(new com.maisonglow.service.SettingsService(new com.maisonglow.dao.SettingDaoSqlite(upgraded.database())).businessName()).isEqualTo("Salón conservado");
        assertThat(customers(open()).listAll()).hasSize(1);
        assertThat(backup).exists();
        long versions = upgraded.database().transaction(c -> {
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT count(*) FROM schema_migration")) {
                r.next(); return r.getLong(1);
            }
        });
        assertThat(versions).isEqualTo(2);
    }

    @Test void conflictingLegacyUsernamesFailUpgradeWithoutLosingBusinessData() throws Exception {
        var app = open(); customer(app, "alice");
        app.database().transaction(c -> {
            try (var s = c.createStatement()) {
                s.executeUpdate("DELETE FROM schema_migration");
                s.execute("DROP INDEX idx_app_user_normalized_username");
                s.execute("ALTER TABLE app_user DROP COLUMN auth_version");
                s.executeUpdate("INSERT INTO app_user(username,password_hash,role) VALUES(' ALICE ','legacy','ADMIN')");
            }
            return null;
        });
        assertThatThrownBy(this::open).isInstanceOf(IllegalStateException.class);
        assertThat(customers(app).listAll()).hasSize(1);
        boolean columnExists = app.database().transaction(c -> {
            try (var s = c.createStatement(); var r = s.executeQuery("PRAGMA table_info(app_user)")) {
                while (r.next()) if (r.getString("name").equals("auth_version")) return true;
                return false;
            }
        });
        assertThat(columnExists).isFalse(); // ALTER and migration record rolled back as one unit
    }

    private static final class MutableClock extends Clock {
        private Instant now = CLOCK.instant();
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
