package com.maisonglow.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("View enum tests")
class ViewTest {

    @Test
    @DisplayName("every screen has a title and its own fxml under the view package")
    void everyScreenHasTitleAndOwnFxml() {
        for (View view : View.values()) {
            assertThat(view.title()).isNotBlank();
            assertThat(view.resource()).startsWith("/com/maisonglow/view/").endsWith("View.fxml");
        }
        assertThat(Arrays.stream(View.values()).map(View::resource).distinct()).hasSize(View.values().length);
    }

    @Test
    @DisplayName("keeps the screens agreed in the contract of corte 3")
    void keepsTheAgreedScreens() {
        assertThat(View.values()).extracting(Enum::name).contains("CUSTOMERS", "PROFESSIONALS", "APPOINTMENTS",
            "PRODUCTS", "SALES", "STATISTICS", "SETTINGS", "ASSISTANT", "TRY_ON");
    }
}
