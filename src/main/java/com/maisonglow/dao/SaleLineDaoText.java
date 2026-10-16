package com.maisonglow.dao;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Product;
import com.maisonglow.model.SaleLine;
import com.maisonglow.model.Sellable;
import com.maisonglow.model.Treatment;

import java.nio.file.Path;
import java.util.List;

/**
 * Stores the lines of every sale in a text file with the columns of the
 * {@code sale_line} table:
 * {@code id|sale_id|product_id|treatment_id|promotion_id|quantity|unit_price}.
 * Only {@link SaleDaoText} uses it, so lines are always saved and removed
 * together with their sale.
 *
 * <p>The table has one column per kind of item, so this is the only place
 * that tells products and treatments apart; sales and lines treat them alike
 * through {@link Sellable}. Promotions arrive in the second delivery, so
 * {@code promotion_id} is always empty.</p>
 */
class SaleLineDaoText extends TextFileDao<SaleLineDaoText.StoredLine> {

    private static final int ID = 0;
    private static final int SALE_ID = 1;
    private static final int PRODUCT_ID = 2;
    private static final int TREATMENT_ID = 3;
    private static final int PROMOTION_ID = 4;
    private static final int QUANTITY = 5;
    private static final int UNIT_PRICE = 6;
    private static final int FIELD_COUNT = 7;
    private static final String NO_VALUE = "";

    private final CrudDao<Product> products;
    private final CrudDao<Treatment> treatments;

    SaleLineDaoText(Path file, CrudDao<Product> products, CrudDao<Treatment> treatments) {
        super(file);
        this.products = products;
        this.treatments = treatments;
    }

    List<SaleLine> findLinesOf(int saleId) throws DataAccessException {
        return storedLinesOf(saleId).stream().map(stored -> stored.line).toList();
    }

    void addLinesOf(int saleId, List<SaleLine> lines) throws DataAccessException {
        for (SaleLine line : lines) {
            create(new StoredLine(saleId, line));
        }
    }

    void deleteLinesOf(int saleId) throws DataAccessException {
        for (StoredLine stored : storedLinesOf(saleId)) {
            delete(stored.id);
        }
    }

    private List<StoredLine> storedLinesOf(int saleId) throws DataAccessException {
        return findAll().stream().filter(stored -> stored.saleId == saleId).toList();
    }

    @Override
    protected String[] toFields(StoredLine stored) {
        SaleLine line = stored.line;
        String[] fields = new String[FIELD_COUNT];
        fields[ID] = String.valueOf(stored.id);
        fields[SALE_ID] = String.valueOf(stored.saleId);
        fields[PRODUCT_ID] = NO_VALUE;
        fields[TREATMENT_ID] = NO_VALUE;
        fields[columnOf(line.getItem())] = String.valueOf(line.getItem().getId());
        fields[PROMOTION_ID] = NO_VALUE;
        fields[QUANTITY] = String.valueOf(line.getQuantity());
        fields[UNIT_PRICE] = String.valueOf(line.getUnitPrice());
        return fields;
    }

    private static int columnOf(Sellable item) {
        if (item instanceof Product) {
            return PRODUCT_ID;
        }
        if (item instanceof Treatment) {
            return TREATMENT_ID;
        }
        throw new IllegalArgumentException("La tabla sale_line no admite ítems de tipo "
            + item.getClass().getSimpleName() + ".");
    }

    @Override
    protected StoredLine fromFields(String[] fields) throws DataAccessException {
        if (fields.length != FIELD_COUNT) {
            throw new DataAccessException("Se esperaban " + FIELD_COUNT + " campos y hay " + fields.length + ".");
        }
        StoredLine stored = new StoredLine(Integer.parseInt(fields[SALE_ID]), restoreLine(fields));
        stored.id = Integer.parseInt(fields[ID]);
        return stored;
    }

    private SaleLine restoreLine(String[] fields) throws DataAccessException {
        Sellable item = findItem(fields[PRODUCT_ID], fields[TREATMENT_ID]);
        try {
            return SaleLine.restore(item, Integer.parseInt(fields[QUANTITY]), Double.parseDouble(fields[UNIT_PRICE]));
        } catch (BusinessRuleException e) {
            throw new DataAccessException("La línea de venta guardada no es válida: " + e.getMessage(), e);
        }
    }

    private Sellable findItem(String productId, String treatmentId) throws DataAccessException {
        if (productId.isEmpty() == treatmentId.isEmpty()) {
            throw new DataAccessException("La línea de venta debe tener un producto o un tratamiento, no ambos ni ninguno.");
        }
        if (productId.isEmpty()) {
            return findRelated(treatments, treatmentId, "tratamiento");
        }
        return findRelated(products, productId, "producto");
    }

    @Override
    protected int getId(StoredLine stored) {
        return stored.id;
    }

    @Override
    protected void setId(StoredLine stored, int id) {
        stored.id = id;
    }

    /** A sale line together with the ids it has in the {@code sale_line} table. */
    static final class StoredLine {

        private int id;
        private final int saleId;
        private final SaleLine line;

        StoredLine(int saleId, SaleLine line) {
            this.saleId = saleId;
            this.line = line;
        }
    }
}
