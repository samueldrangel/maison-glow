package com.maisonglow.service;

import com.maisonglow.dao.ProductDao;
import com.maisonglow.dao.SaleDao;
import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Appointment;
import com.maisonglow.model.Customer;
import com.maisonglow.model.Product;
import com.maisonglow.model.Sale;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sellable;
import com.maisonglow.model.Treatment;
import com.maisonglow.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Owns sale composition, totals, appointment links and inventory coordination. */
public class SaleService {
    private final SaleDao saleDao;
    private final ProductDao productDao;

    public SaleService(SaleDao saleDao, ProductDao productDao) {
        this.saleDao = Objects.requireNonNull(saleDao, "saleDao");
        this.productDao = Objects.requireNonNull(productDao, "productDao");
    }

    public static SaleLine addLine(Sale sale, Sellable item, int quantity) throws BusinessRuleException {
        Objects.requireNonNull(sale, "sale"); Objects.requireNonNull(item, "item");
        if (sale.getId() != 0) throw new BusinessRuleException("La venta ya fue registrada.");
        requireLine(item, quantity, item.getPrice());
        for (SaleLine line : sale.getLines()) {
            if (line.getItem().equals(item)) {
                try { line.setQuantity(Math.addExact(line.getQuantity(), quantity)); }
                catch (ArithmeticException e) { throw new BusinessRuleException("La cantidad excede el máximo permitido."); }
                sale.setTotal(calculateTotal(sale));
                return line;
            }
        }
        SaleLine line = new SaleLine(item, quantity, item.getPrice());
        List<SaleLine> lines = new ArrayList<>(sale.getLines());
        lines.add(line); sale.setLines(lines); sale.setTotal(calculateTotal(sale));
        return line;
    }

    public static BigDecimal calculateSubtotal(SaleLine line) {
        return line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
    }

    public static BigDecimal calculateTotal(Sale sale) {
        return sale.getLines().stream().map(SaleService::calculateSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Builds historical fixtures/imports with a total calculated by the business layer. */
    public static Sale restoreSale(int id, Customer customer, LocalDateTime soldAt, List<SaleLine> lines) {
        Sale sale = Sale.restore(id, customer, soldAt, lines, BigDecimal.ZERO);
        sale.setTotal(calculateTotal(sale));
        return sale;
    }

    public static void linkAppointment(Sale sale, Appointment appointment) throws BusinessRuleException {
        Objects.requireNonNull(appointment, "appointment");
        if (sale.getAppointment().isPresent()) throw new BusinessRuleException("La venta ya está enlazada a una cita.");
        if (!appointment.getCustomer().equals(sale.getCustomer())) throw new BusinessRuleException("La cita es de otro cliente.");
        sale.setAppointment(appointment);
    }

    public Sale register(Sale sale) throws ValidationException, BusinessRuleException, DataAccessException {
        Validator.requirePresent(sale, "venta");
        if (sale.getLines().isEmpty()) throw new ValidationException("La venta debe tener al menos una línea.");
        if (sale.getId() != 0) throw new BusinessRuleException("La venta ya fue registrada.");
        if (sale.getAppointment().isPresent()
            && !sale.getAppointment().orElseThrow().getCustomer().equals(sale.getCustomer())) {
            throw new BusinessRuleException("La cita es de otro cliente.");
        }
        Map<Integer, Product> originals = new LinkedHashMap<>();
        Map<Integer, Integer> quantities = new LinkedHashMap<>();
        for (SaleLine line : sale.getLines()) {
            requireLine(line.getItem(), line.getQuantity(), line.getUnitPrice());
            if (line.getItem() instanceof Product product) {
                Product current = productDao.findById(product.getId()).orElseThrow(() ->
                    new BusinessRuleException("No existe el producto con id " + product.getId() + "."));
                if (!current.isActive()) throw new BusinessRuleException("El producto " + current.getName() + " no está activo.");
                originals.put(current.getId(), current);
                try { quantities.merge(current.getId(), line.getQuantity(), Math::addExact); }
                catch (ArithmeticException e) { throw new BusinessRuleException("La cantidad excede el máximo permitido."); }
            }
        }
        Map<Integer, Product> updates = new LinkedHashMap<>();
        for (Product current : originals.values()) {
            Product changed = new Product(current.getId(), current.getName(), current.getCategory(), current.getPrice(),
                current.getStock(), current.getMinimumStock());
            changed.setActive(current.isActive());
            ProductService.deductStock(changed, quantities.get(current.getId()));
            updates.put(changed.getId(), changed);
        }
        sale.setTotal(calculateTotal(sale));
        List<Product> saved = new ArrayList<>();
        Sale stored;
        try {
            for (Product changed : updates.values()) {
                productDao.update(changed); saved.add(changed);
            }
            stored = saleDao.create(sale);
        } catch (DataAccessException e) {
            for (int i = saved.size() - 1; i >= 0; i--) {
                try { productDao.update(originals.get(saved.get(i).getId())); }
                catch (DataAccessException undoError) { if (undoError != e) e.addSuppressed(undoError); }
            }
            throw e;
        }
        for (SaleLine line : sale.getLines()) {
            if (line.getItem() instanceof Product product) product.setStock(updates.get(product.getId()).getStock());
        }
        return stored;
    }

    private static void requireLine(Sellable item, int quantity, BigDecimal price) throws BusinessRuleException {
        if (quantity <= 0) throw new BusinessRuleException("La cantidad debe ser mayor que cero.");
        if (price == null || price.signum() <= 0) throw new BusinessRuleException("El precio de " + item.getName() + " debe ser mayor que cero.");
        if (!(item instanceof Product) && !(item instanceof Treatment)) {
            throw new BusinessRuleException("El tipo de ítem no está soportado.");
        }
    }

    public List<Sale> listByCustomer(int customerId) throws ValidationException, DataAccessException {
        Validator.requirePositive(customerId, "cliente");
        return saleDao.findByCustomer(customerId);
    }
}
