package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Historical totals and line prices are preserved; sale and lines share a transaction. */
public final class SaleDaoSqlite extends SqliteCrudDao<Sale> implements SaleDao {
    private final CustomerDao customers;
    private final AppointmentDao appointments;
    private final ProductDao products;
    private final TreatmentDao treatments;
    public SaleDaoSqlite(SqliteDatabase database, CustomerDao customers, AppointmentDao appointments, ProductDao products, TreatmentDao treatments) {
        super(database,"sale","id","customer_id","appointment_id","sold_at","total");
        this.customers=customers; this.appointments=appointments; this.products=products; this.treatments=treatments;
    }
    @Override protected String[] toFields(Sale sale) {
        return new String[]{""+sale.getId(),""+sale.getCustomer().getId(),sale.getAppointment().map(a -> ""+a.getId()).orElse(""),
            sale.getSoldAt().toString(),sale.getTotal().toPlainString()};
    }
    @Override protected Sale fromFields(String[] f) throws DataAccessException {
        int id = Integer.parseInt(f[0]);
        List<SaleLine> lines = database.transaction(c -> {
            List<SaleLine> result = new ArrayList<>();
            try(var statement = c.prepareStatement("SELECT product_id,treatment_id,quantity,unit_price FROM sale_line WHERE sale_id=? ORDER BY id")) {
                statement.setInt(1,id);
                try(var rows=statement.executeQuery()) {
                    while(rows.next()) {
                        Sellable item = rows.getString(1)==null ? findRelated(treatments,rows.getString(2),"tratamiento") : findRelated(products,rows.getString(1),"producto");
                        result.add(SaleLine.restore(item,rows.getInt(3),new BigDecimal(rows.getString(4))));
                    }
                }
            }
            return result;
        });
        Sale sale = Sale.restore(id,findRelated(customers,f[1],"cliente"),LocalDateTime.parse(f[3]),lines,new BigDecimal(f[4]));
        if(!f[2].isEmpty()) sale.setAppointment(findRelated(appointments,f[2],"cita"));
        return sale;
    }
    @Override protected void saveRelations(Connection c, Sale sale) throws SQLException {
        try(var delete=c.prepareStatement("DELETE FROM sale_line WHERE sale_id=?")) { delete.setInt(1,sale.getId()); delete.executeUpdate(); }
        try(var insert=c.prepareStatement("INSERT INTO sale_line(sale_id,product_id,treatment_id,quantity,unit_price) VALUES (?,?,?,?,?)")) {
            for(SaleLine line:sale.getLines()) {
                insert.setInt(1,sale.getId());
                insert.setObject(2,line.getItem() instanceof Product ? line.getItem().getId():null);
                insert.setObject(3,line.getItem() instanceof Treatment ? line.getItem().getId():null);
                insert.setInt(4,line.getQuantity()); insert.setString(5,line.getUnitPrice().toPlainString()); insert.addBatch();
            }
            insert.executeBatch();
        }
    }
    @Override protected int getId(Sale sale) { return sale.getId(); }
    @Override protected void setId(Sale sale,int id) { sale.setId(id); }
    @Override public List<Sale> findByCustomer(int id) throws DataAccessException { return query(" WHERE customer_id=? ORDER BY sold_at",id); }
    @Override public List<Sale> findBetween(LocalDate from,LocalDate to) throws DataAccessException {
        return query(" WHERE sold_at>=? AND sold_at<? ORDER BY sold_at",from.atStartOfDay().toString(),to.plusDays(1).atStartOfDay().toString());
    }
}
