package com.maisonglow.view.customer;

import com.maisonglow.model.Customer;
import com.maisonglow.service.CustomerStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomerProfile tests")
class CustomerProfileTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    private static CustomerProfile profile(String name, LocalDate registered, CustomerStatistics statistics) {
        return profile(name, registered, statistics, 0);
    }

    private static CustomerProfile profile(String name, LocalDate registered, CustomerStatistics statistics, int noShows) {
        return new CustomerProfile(new Customer(1, "1001", name, "3001112233", "a@correo.com", registered, noShows), statistics, TODAY);
    }

    private static final CustomerStatistics NOTHING = new CustomerStatistics(0, 0, 0, 0, 0);

    @Test
    @DisplayName("the initials are the first letters of the first two words, in capitals")
    void initials() {
        assertThat(profile("ana maría García", TODAY, NOTHING).initials()).isEqualTo("AM");
        assertThat(profile("  Laura  ", TODAY, NOTHING).initials()).isEqualTo("L");
    }

    @Test
    @DisplayName("the average ticket is the total spent over the purchases, and zero without purchases")
    void averageTicket() {
        assertThat(profile("Ana", TODAY, new CustomerStatistics(3, 90000, 0, 0, 0)).averageTicket()).isEqualByComparingTo("30000");
        assertThat(profile("Ana", TODAY, NOTHING).averageTicket()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("attendance counts completed against missed appointments and ignores the ones still to come")
    void attendance() {
        assertThat(profile("Ana", TODAY, new CustomerStatistics(0, 0, 6, 3, 0), 1).attendance().orElseThrow()).isEqualTo(0.75);
        assertThat(profile("Ana", TODAY, NOTHING, 2).noShows()).isEqualTo(2);
        assertThat(profile("Ana", TODAY, new CustomerStatistics(0, 0, 2, 0, 0)).attendance()).isEmpty();
    }

    @Test
    @DisplayName("a customer is new during 30 days after registering, the 30th included")
    void isNew() {
        assertThat(profile("Ana", TODAY.minusDays(30), NOTHING).isNew()).isTrue();
        assertThat(profile("Ana", TODAY.minusDays(31), NOTHING).isNew()).isFalse();
    }
}
