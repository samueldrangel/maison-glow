package com.maisonglow.service;

import com.maisonglow.dao.CustomerDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService tests")
class CustomerServiceTest {

    @Mock
    private CustomerDao customerDao;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customerDao);
    }

    private Customer ana() {
        return new Customer(1, "1065123456", "Ana Pérez", "3001234567", "ana@correo.com");
    }

    @Test
    @DisplayName("register stores a valid customer with trimmed data")
    void registerValidCustomer() throws Exception {
        when(customerDao.findByDocument("1065123456")).thenReturn(Optional.empty());
        when(customerDao.create(any(Customer.class))).thenAnswer(call -> call.getArgument(0));

        Customer customer = service.register(
            new ContactData(" 1065123456 ", " Ana Pérez ", "3001234567", "ana@correo.com"));

        assertThat(customer.getDocument()).isEqualTo("1065123456");
        assertThat(customer.getName()).isEqualTo("Ana Pérez");
        assertThat(customer.getNoShowCount()).isZero();
    }

    @Test
    @DisplayName("register rejects a repeated document")
    void registerRepeatedDocument() throws Exception {
        when(customerDao.findByDocument("1065123456")).thenReturn(Optional.of(ana()));

        assertThatThrownBy(() -> service.register(
            new ContactData("1065123456", "Otra", "3001234567", "otra@correo.com")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("1065123456");
        verify(customerDao, never()).create(any());
    }

    @Test
    @DisplayName("register rejects malformed data before touching storage")
    void registerInvalidData() throws Exception {
        assertThatThrownBy(() -> service.register(new ContactData("", "Ana", "3001234567", "ana@correo.com")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new ContactData("1065", "Ana", "12", "ana@correo.com")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.register(new ContactData("1065", "Ana", "3001234567", "ana-correo")))
            .isInstanceOf(ValidationException.class);
        verify(customerDao, never()).create(any());
    }

    @Test
    @DisplayName("findById fails for an unknown customer")
    void findUnknownCustomer() throws Exception {
        when(customerDao.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("99");
    }

    @Test
    @DisplayName("searchByName ignores case and matches partial names")
    void searchByName() throws Exception {
        Customer luis = new Customer(2, "77111222", "Luis Díaz", "3012223344", "luis@correo.com");
        when(customerDao.findAll()).thenReturn(List.of(ana(), luis));

        assertThat(service.searchByName("pér")).containsExactly(ana());
        assertThat(service.searchByName("DÍAZ")).containsExactly(luis);
        assertThatThrownBy(() -> service.searchByName(" ")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("update changes the contact data and keeps the document")
    void updateCustomer() throws Exception {
        when(customerDao.findById(1)).thenReturn(Optional.of(ana()));

        Customer updated = service.update(1,
            new ContactData("1065123456", "Ana María Pérez", "3109998877", "ana.maria@correo.com"));

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerDao).update(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ana María Pérez");
        assertThat(updated.getDocument()).isEqualTo("1065123456");
        assertThat(updated.getEmail()).isEqualTo("ana.maria@correo.com");
    }

    @Test
    @DisplayName("update rejects invalid data")
    void updateInvalidData() throws Exception {
        assertThatThrownBy(() -> service.update(1, new ContactData("1065123456", "Ana", "3001234567", "mal")))
            .isInstanceOf(ValidationException.class);
        verify(customerDao, never()).update(any());
    }

    @Test
    @DisplayName("update rejects a different document")
    void updateDifferentDocument() throws Exception {
        when(customerDao.findById(1)).thenReturn(Optional.of(ana()));

        assertThatThrownBy(() -> service.update(1, new ContactData("999", "Ana", "3001234567", "ana@correo.com")))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("documento");
        verify(customerDao, never()).update(any());
    }

    @Test
    @DisplayName("registerNoShow increments and stores the counter")
    void registerNoShow() throws Exception {
        Customer customer = ana();
        when(customerDao.findById(1)).thenReturn(Optional.of(customer));

        service.registerNoShow(1);

        assertThat(customer.getNoShowCount()).isEqualTo(1);
        verify(customerDao).update(customer);
    }

    @Test
    @DisplayName("delete removes an existing customer and rejects unknown ones")
    void deleteCustomer() throws Exception {
        when(customerDao.findById(1)).thenReturn(Optional.of(ana()));
        when(customerDao.findById(5)).thenReturn(Optional.empty());

        service.delete(1);

        verify(customerDao).delete(1);
        assertThatThrownBy(() -> service.delete(5)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("listAll and findByDocument delegate to storage")
    void listAndFindByDocument() throws Exception {
        when(customerDao.findAll()).thenReturn(List.of(ana()));
        when(customerDao.findByDocument("1065123456")).thenReturn(Optional.of(ana()));

        assertThat(service.listAll()).containsExactly(ana());
        assertThat(service.findByDocument("1065123456")).contains(ana());
    }
}
