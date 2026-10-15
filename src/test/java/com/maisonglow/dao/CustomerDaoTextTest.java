package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CustomerDaoText tests")
class CustomerDaoTextTest {

    @TempDir
    Path folder;

    private Customer ana() {
        return new Customer(0, "1065123456", "Ana Pérez", "3001234567", "ana@correo.com",
            LocalDate.of(2026, 9, 1), 1);
    }

    @Test
    @DisplayName("create assigns ids and the data survives a reload")
    void createAndReload() throws DataAccessException {
        Path file = folder.resolve("customers.txt");
        CustomerDao dao = new CustomerDaoText(file);

        Customer created = dao.create(ana());

        Customer reloaded = new CustomerDaoText(file).findById(created.getId()).orElseThrow();
        assertThat(created.getId()).isEqualTo(1);
        assertThat(reloaded.getName()).isEqualTo("Ana Pérez");
        assertThat(reloaded.getRegisteredAt()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(reloaded.getNoShowCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByDocument returns the matching customer")
    void findByDocument() throws DataAccessException {
        CustomerDao dao = new CustomerDaoText(folder.resolve("customers.txt"));
        dao.create(ana());

        assertThat(dao.findByDocument("1065123456")).isPresent();
        assertThat(dao.findByDocument("000")).isEmpty();
    }

    @Test
    @DisplayName("update and delete are persisted")
    void updateAndDelete() throws DataAccessException {
        Path file = folder.resolve("customers.txt");
        CustomerDao dao = new CustomerDaoText(file);
        Customer customer = dao.create(ana());
        Customer other = dao.create(new Customer(0, "77111222", "Luis Díaz", "3012223344", "luis@correo.com"));

        customer.setName("Ana | María");
        dao.update(customer);
        dao.delete(other.getId());

        CustomerDao reloaded = new CustomerDaoText(file);
        assertThat(reloaded.findAll()).extracting(Customer::getName).containsExactly("Ana | María");
    }

    @Test
    @DisplayName("deleting an unknown id is reported")
    void deleteUnknown() {
        CustomerDao dao = new CustomerDaoText(folder.resolve("customers.txt"));

        assertThatThrownBy(() -> dao.delete(42)).isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("a line with missing fields is reported with its line number")
    void malformedLine() throws Exception {
        Path file = folder.resolve("customers.txt");
        Files.writeString(file, "#nextId=2\n1|1065123456|Ana Pérez\n");

        assertThatThrownBy(() -> new CustomerDaoText(file).findAll())
            .isInstanceOf(DataAccessException.class)
            .hasMessageContaining("línea 2");
    }
}
