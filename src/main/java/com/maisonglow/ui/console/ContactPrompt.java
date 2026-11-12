package com.maisonglow.ui.console;

import com.maisonglow.service.ContactData;

/**
 * Asks for the contact data shared by customers and professionals, so both
 * modules ask the same questions in the same order.
 */
final class ContactPrompt {

    private ContactPrompt() {
    }

    static ContactData ask(ConsoleIo console) {
        String document = console.readLine("Documento");
        String name = console.readLine("Nombre completo");
        String phone = console.readLine("Teléfono");
        String email = console.readLine("Correo electrónico");
        return new ContactData(document, name, phone, email);
    }
}
