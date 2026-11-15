package com.maisonglow.view.sales;

import com.maisonglow.view.FxTest;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("Invoice dialog tests")
class InvoiceDialogTest {

    @Test
    @DisplayName("offers to send the invoice by e-mail or to close it, and shows its text")
    void offersSendAndClose() throws Exception {
        assumeTrue(FxTest.start(), "JavaFX toolkit needs a display");

        Dialog<ButtonType> dialog = FxTest.onFxThread(() -> InvoiceDialog.build("Factura de venta N.° 7"));

        assertThat(dialog.getDialogPane().getButtonTypes()).containsExactly(InvoiceDialog.SEND, InvoiceDialog.CLOSE);
        assertThat(dialog.getDialogPane().getContent().lookup(".invoice-text")).isNotNull();
    }
}
