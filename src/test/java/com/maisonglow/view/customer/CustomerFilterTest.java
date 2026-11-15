package com.maisonglow.view.customer;

import com.maisonglow.model.Customer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerFilter tests")
class CustomerFilterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    private final Customer ana = new Customer(1, "1001", "Ana García", "3001112233", "ana@correo.com", TODAY.minusDays(200), 0);
    private final Customer laura = new Customer(2, "1002", "Laura Méndez", "3124445566", "laura@correo.com", TODAY.minusDays(40), 1);
    private final Customer paula = new Customer(3, "2003", "Paula Andrés", "3207778899", "paula@correo.com", TODAY.minusDays(10), 2);
    private final List<Customer> customers = List.of(ana, laura, paula);

    private List<Customer> shown(CustomerFilter filter) {
        return customers.stream().filter(filter::matches).toList();
    }

    @Test
    @DisplayName("lets everything through when nothing is chosen")
    void allPasses() {
        assertThat(shown(CustomerFilter.all(TODAY))).containsExactly(ana, laura, paula);
    }

    @Test
    @DisplayName("searches by name ignoring case and accents")
    void searchesByName() {
        assertThat(shown(CustomerFilter.all(TODAY).withText("GARCIA"))).containsExactly(ana);
    }

    @Test
    @DisplayName("searches by document and by phone")
    void searchesByDocumentAndPhone() {
        assertThat(shown(CustomerFilter.all(TODAY).withText("2003"))).containsExactly(paula);
        assertThat(shown(CustomerFilter.all(TODAY).withText("312444"))).containsExactly(laura);
    }

    @Test
    @DisplayName("the no-shows chip keeps only customers who missed an appointment")
    void withNoShows() {
        assertThat(shown(CustomerFilter.all(TODAY).withGroup(CustomerFilter.Group.WITH_NO_SHOWS))).containsExactly(laura, paula);
    }

    @Test
    @DisplayName("the new chip keeps customers registered in the last 30 days, the 30th day included")
    void newCustomers() {
        Customer onTheLimit = new Customer(4, "3004", "Marta Gil", "3011112222", "marta@correo.com", TODAY.minusDays(30), 0);

        assertThat(shown(CustomerFilter.all(TODAY).withGroup(CustomerFilter.Group.NEW))).containsExactly(paula);
        assertThat(CustomerFilter.all(TODAY).withGroup(CustomerFilter.Group.NEW).matches(onTheLimit)).isTrue();
    }

    @Test
    @DisplayName("the search and the chip work together")
    void combines() {
        assertThat(shown(CustomerFilter.all(TODAY).withText("paula").withGroup(CustomerFilter.Group.WITH_NO_SHOWS)))
            .containsExactly(paula);
        assertThat(shown(CustomerFilter.all(TODAY).withText("ana").withGroup(CustomerFilter.Group.WITH_NO_SHOWS))).isEmpty();
    }
}
