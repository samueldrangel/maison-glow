package com.maisonglow.model;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class ModelBoundaryTest {
    @Test void modelsExposeDataRatherThanBusinessOperations() {
        Set<String> operations = Set.of("addLine", "calculateTotal", "calculateSubtotal", "linkAppointment",
            "hasStock", "deductStock", "addStock", "isBelowMinimum", "confirm", "cancel", "complete",
            "markNoShow", "registerNoShow", "rescheduledTo", "overlapsWith", "blocksSchedule", "canChangeTo",
            "worksDuring", "offers", "calculateEnd");
        for (Class<?> model : new Class<?>[] { Sale.class, SaleLine.class, Product.class, Treatment.class,
                Customer.class, Professional.class, Appointment.class, AppointmentStatus.class }) {
            for (Method method : model.getDeclaredMethods()) {
                assertThat(operations).as(model.getSimpleName() + "." + method.getName()).doesNotContain(method.getName());
            }
        }
    }
}
