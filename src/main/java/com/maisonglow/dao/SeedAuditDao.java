package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Persistence and integrity checks for explicitly owned demonstration databases. */
public final class SeedAuditDao {
    public static final String SIGNATURE_KEY="local_demo_seed_v1";
    private static final String[] TABLES={"customer","app_user","product_category","product","treatment","professional","appointment","sale","customer_order","customer_change"};
    private final SqliteDatabase db;
    public SeedAuditDao(SqliteDatabase db){this.db=db;}
    public Map<String,Long> counts()throws DataAccessException {
        return db.transaction(c -> {
            Map<String,Long> counts=new LinkedHashMap<>();
            for(String table:TABLES)try(var s=c.createStatement();var r=s.executeQuery("SELECT count(*) FROM "+table)){r.next();counts.put(table,r.getLong(1));}
            return java.util.Collections.unmodifiableMap(counts);
        });
    }
    public Optional<String> signature()throws DataAccessException {
        return db.transaction(c -> {try(var s=c.prepareStatement("SELECT value FROM app_metadata WHERE key=?")){s.setString(1,SIGNATURE_KEY);try(var r=s.executeQuery()){return r.next()?Optional.of(r.getString(1)):Optional.empty();}}});
    }
    public void complete(String signature)throws DataAccessException {
        db.transaction(c -> {try(var s=c.prepareStatement("INSERT INTO app_metadata VALUES(?,?)")){s.setString(1,SIGNATURE_KEY);s.setString(2,signature);s.executeUpdate();}return null;});
    }
    public void restock(int productId,int quantity,int adminId,LocalDateTime now)throws DataAccessException {
        if(quantity==0)return;
        db.transaction(c -> {try(var s=c.prepareStatement("INSERT INTO stock_movement(product_id,movement_type,quantity,reason,created_by,created_at) VALUES(?,'RESTOCK',?,'Existencias ficticias de demo',?,?)")){
            s.setInt(1,productId);s.setInt(2,quantity);s.setInt(3,adminId);s.setString(4,now.toString());s.executeUpdate();}return null;});
    }
    /** Historical sale import: the stock movement trigger, not SaleService, applies these fixture deductions. */
    public void saleMovements(int saleId,int adminId,LocalDateTime now)throws DataAccessException {
        db.transaction(c -> {try(var s=c.prepareStatement("INSERT INTO stock_movement(product_id,movement_type,quantity,reason,sale_line_id,created_by,created_at) SELECT product_id,'SALE',-quantity,'Venta ficticia de demo',id,?,? FROM sale_line WHERE sale_id=? AND product_id IS NOT NULL")){
            s.setInt(1,adminId);s.setString(2,now.toString());s.setInt(3,saleId);s.executeUpdate();}return null;});
    }
    public void accountDates(LocalDateTime now)throws DataAccessException {
        db.transaction(c -> {try(var s=c.prepareStatement("UPDATE app_user SET created_at=?")){s.setString(1,now.toString());s.executeUpdate();}return null;});
    }
    public void validate()throws DataAccessException {
        db.transaction(c -> {
            try(var s=c.createStatement();var r=s.executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new DataAccessException("La semilla tiene relaciones inválidas.");}
            for(String query:new String[]{
                "SELECT 1 FROM product p WHERE stock != COALESCE((SELECT SUM(quantity) FROM stock_movement WHERE product_id=p.id),0) LIMIT 1",
                "SELECT 1 FROM product p WHERE stock < COALESCE((SELECT SUM(i.quantity) FROM order_item i JOIN customer_order o ON o.id=i.order_id WHERE i.product_id=p.id AND o.status='PENDING_PAYMENT'),0) LIMIT 1",
                "SELECT 1 FROM customer c WHERE no_show_count != (SELECT count(*) FROM appointment a WHERE a.customer_id=c.id AND a.status='NO_SHOW') LIMIT 1",
                "SELECT 1 FROM customer_order o JOIN sale s ON o.sale_id=s.id WHERE o.customer_id != s.customer_id OR o.status!='PAID' OR o.total!=s.total LIMIT 1",
                "SELECT 1 FROM customer_order WHERE (status='PAID') != (sale_id IS NOT NULL) LIMIT 1"
                ,"SELECT 1 FROM appointment a JOIN appointment b ON a.id<b.id AND (a.customer_id=b.customer_id OR a.professional_id=b.professional_id) AND a.start_at<b.end_at AND b.start_at<a.end_at WHERE a.status IN ('PENDING','CONFIRMED') AND b.status IN ('PENDING','CONFIRMED') LIMIT 1"
                ,"SELECT 1 FROM customer_order o WHERE ABS(CAST(o.total AS NUMERIC)-(SELECT SUM(i.quantity*CAST(i.unit_price AS NUMERIC)) FROM order_item i WHERE i.order_id=o.id))>0.001 LIMIT 1"
            })try(var s=c.createStatement();var r=s.executeQuery(query)){if(r.next())throw new DataAccessException("La semilla no conserva stock, propietarios o estados coherentes: "+query);}
            try(var s=c.createStatement();var r=s.executeQuery("SELECT s.id,s.total,l.quantity,l.unit_price FROM sale s JOIN sale_line l ON l.sale_id=s.id ORDER BY s.id,l.id")){
                int sale=-1;BigDecimal expected=BigDecimal.ZERO,total=BigDecimal.ZERO;
                while(r.next()){
                    if(sale!=r.getInt(1)){
                        if(sale!=-1&&total.compareTo(expected)!=0)throw new DataAccessException("Total histórico de venta inválido.");
                        sale=r.getInt(1);expected=new BigDecimal(r.getString(2));total=BigDecimal.ZERO;
                    }
                    total=total.add(new BigDecimal(r.getString(4)).multiply(BigDecimal.valueOf(r.getInt(3))));
                }
                if(sale!=-1&&total.compareTo(expected)!=0)throw new DataAccessException("Total histórico de venta inválido.");
            }
            return null;
        });
    }
}
