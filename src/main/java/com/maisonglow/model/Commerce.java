package com.maisonglow.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Immutable snapshots for the customer-facing services; no persistence or business operations. */
public final class Commerce {
    private Commerce() { }
    public enum OrderStatus { PENDING_PAYMENT, PAID, CANCELLED, EXPIRED }
    public enum ChangeKind { PERSONAL, SERVICE, SIMULATION }
    public record CatalogProduct(int id, String name, String category, BigDecimal price,
                                 int availableStock, boolean published, boolean hasCover) { }
    public record OrderLine(int productId, String name, int quantity, BigDecimal unitPrice) { }
    public record Order(int id, int customerId, String requestKey, OrderStatus status, LocalDateTime createdAt,
                        LocalDateTime expiresAt, Integer saleId, BigDecimal total, List<OrderLine> lines) {
        public Order { lines = List.copyOf(lines); }
    }
    public record Change(int id, int customerId, int authorId, Integer appointmentId, ChangeKind kind,
                         String notes, LocalDate happenedOn, LocalDateTime createdAt, String sourceKey, String resultKey) { }
    /** Does not disclose a professional's document, phone, email or full agenda. */
    public record ProfessionalOption(int id, String name, String specialty) { }
}
