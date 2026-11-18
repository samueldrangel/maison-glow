package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.model.Commerce.*;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Prepared SQLite queries for publication, reservations and history; decisions stay in services. */
public final class CommerceDao {
    private final SqliteDatabase db;
    public CommerceDao(SqliteDatabase db) { this.db = db; }
    public record Listing(boolean published, String coverKey) { }

    public Listing listing(int productId) throws DataAccessException {
        return db.transaction(c -> {
            try(var s=c.prepareStatement("SELECT * FROM product_listing WHERE product_id=?")) {
                s.setInt(1,productId);
                try(var r=s.executeQuery()) { return r.next()?new Listing(r.getBoolean("published"),r.getString("cover_key")):new Listing(false,null); }
            }
        });
    }
    public void saveListing(int productId, boolean published, String key) throws DataAccessException {
        execute("INSERT INTO product_listing VALUES(?,?,?) ON CONFLICT(product_id) DO UPDATE SET published=excluded.published,cover_key=excluded.cover_key",productId,published,key);
    }
    public int reserved(int productId) throws DataAccessException {
        return db.transaction(c -> {
            try(var s=c.prepareStatement("SELECT COALESCE(SUM(i.quantity),0) FROM order_item i JOIN customer_order o ON o.id=i.order_id WHERE i.product_id=? AND o.status='PENDING_PAYMENT'")) {
                s.setInt(1,productId); try(var r=s.executeQuery()) { r.next(); return r.getInt(1); }
            }
        });
    }
    public void expire(LocalDateTime now) throws DataAccessException {
        execute("UPDATE customer_order SET status='EXPIRED' WHERE status='PENDING_PAYMENT' AND expires_at<=?",now.toString());
    }
    public Optional<Order> order(int id) throws DataAccessException { return ordersWhere("WHERE id=?",id).stream().findFirst(); }
    public Optional<Order> byRequest(int customerId,String key) throws DataAccessException {
        return ordersWhere("WHERE customer_id=? AND request_key=?",customerId,key).stream().findFirst();
    }
    public List<Order> orders(Integer customerId) throws DataAccessException {
        return customerId==null?ordersWhere("ORDER BY id DESC"):ordersWhere("WHERE customer_id=? ORDER BY id DESC",customerId);
    }
    private List<Order> ordersWhere(String suffix,Object... values) throws DataAccessException {
        return db.transaction(c -> {
            List<Order> result=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT * FROM customer_order "+suffix)) {
                bind(s,values); try(var r=s.executeQuery()) {
                    while(r.next()) {
                        int id=r.getInt("id");
                        int sale=r.getInt("sale_id"); Integer saleId=r.wasNull()?null:sale;
                        result.add(new Order(id,r.getInt("customer_id"),r.getString("request_key"),OrderStatus.valueOf(r.getString("status")),
                            LocalDateTime.parse(r.getString("created_at")),LocalDateTime.parse(r.getString("expires_at")),saleId,new BigDecimal(r.getString("total")),lines(id)));
                    }
                }
            }
            return result;
        });
    }
    private List<OrderLine> lines(int orderId) throws DataAccessException {
        return db.transaction(c -> {
            List<OrderLine> result=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT * FROM order_item WHERE order_id=? ORDER BY product_id")) {
                s.setInt(1,orderId);try(var r=s.executeQuery()) { while(r.next()) result.add(new OrderLine(r.getInt("product_id"),r.getString("name"),r.getInt("quantity"),new BigDecimal(r.getString("unit_price")))); }
            }
            return result;
        });
    }
    public Order insertOrder(int customerId,String requestKey,LocalDateTime now,LocalDateTime expires,BigDecimal total,List<OrderLine> lines) throws DataAccessException {
        return db.transaction(c -> {
            int id;
            try(var s=c.prepareStatement("INSERT INTO customer_order(customer_id,request_key,status,created_at,expires_at,total) VALUES(?,?,'PENDING_PAYMENT',?,?,?) RETURNING id")) {
                bind(s,customerId,requestKey,now.toString(),expires.toString(),total.toPlainString());
                try(var r=s.executeQuery()) { r.next();id=r.getInt(1); }
            }
            for(var line:lines) execute("INSERT INTO order_item VALUES(?,?,?,?,?)",id,line.productId(),line.name(),line.quantity(),line.unitPrice().toPlainString());
            return order(id).orElseThrow();
        });
    }
    public void orderState(int id,OrderStatus status,Integer saleId) throws DataAccessException {
        execute("UPDATE customer_order SET status=?,sale_id=? WHERE id=?",status.name(),saleId,id);
    }
    public void recordPayment(int saleId,String method,BigDecimal amount,LocalDateTime now) throws DataAccessException {
        execute("INSERT INTO payment(sale_id,kind,method,amount,paid_at) VALUES(?,'SALE_PAYMENT',?,?,?)",saleId,method,amount.toPlainString(),now.toString());
    }
    public void recordDeposit(int appointmentId,String method,BigDecimal amount,LocalDateTime now)throws DataAccessException {
        execute("INSERT INTO payment(appointment_id,kind,method,amount,paid_at) VALUES(?,'DEPOSIT',?,?,?)",appointmentId,method,amount.toPlainString(),now.toString());
    }
    public void enqueuePhotoDelete(String key)throws DataAccessException {if(key!=null)execute("INSERT OR IGNORE INTO media_delete_queue VALUES(?)",key);}
    public void photoDeleted(String key)throws DataAccessException {execute("DELETE FROM media_delete_queue WHERE photo_key=?",key);}
    public List<String> pendingPhotoDeletes()throws DataAccessException {
        return db.transaction(c -> {List<String> keys=new ArrayList<>();try(var s=c.createStatement();var r=s.executeQuery("SELECT photo_key FROM media_delete_queue")){while(r.next())keys.add(r.getString(1));}return keys;});
    }
    public boolean staffAccess(int customerId) throws DataAccessException {
        return db.transaction(c -> {
            try(var s=c.prepareStatement("SELECT allowed FROM customer_photo_access WHERE customer_id=?")) {
                s.setInt(1,customerId); try(var r=s.executeQuery()) { return r.next()&&r.getBoolean(1); }
            }
        });
    }
    public void setStaffAccess(int customerId,boolean allowed) throws DataAccessException {
        execute("INSERT INTO customer_photo_access VALUES(?,?) ON CONFLICT(customer_id) DO UPDATE SET allowed=excluded.allowed",customerId,allowed);
    }
    public Change insertChange(int customer,int author,Integer appointment,ChangeKind kind,String notes,LocalDate day,LocalDateTime now,String source,String result) throws DataAccessException {
        return db.transaction(c -> {
            try(var s=c.prepareStatement("INSERT INTO customer_change(customer_id,author_id,appointment_id,kind,notes,happened_on,created_at,source_key,result_key) VALUES(?,?,?,?,?,?,?,?,?) RETURNING *")) {
                bind(s,customer,author,appointment,kind.name(),notes,day.toString(),now.toString(),source,result);
                try(var r=s.executeQuery()) {r.next();return readChange(r);}
            }
        });
    }
    public List<Change> changes(int customerId) throws DataAccessException {
        return db.transaction(c -> {
            List<Change> result=new ArrayList<>();
            try(var s=c.prepareStatement("SELECT * FROM customer_change WHERE customer_id=? ORDER BY happened_on DESC,id DESC")) {
                s.setInt(1,customerId);try(var r=s.executeQuery()) {while(r.next()) result.add(readChange(r));}
            }
            return result;
        });
    }
    public Optional<Change> change(int id) throws DataAccessException {
        return db.transaction(c -> {
            try(var s=c.prepareStatement("SELECT * FROM customer_change WHERE id=?")) {
                s.setInt(1,id);try(var r=s.executeQuery()) {return r.next()?Optional.of(readChange(r)):Optional.empty();}
            }
        });
    }
    public void removePhotos(int id) throws DataAccessException { execute("UPDATE customer_change SET source_key=NULL,result_key=NULL WHERE id=?",id); }
    private static Change readChange(ResultSet r) throws SQLException {
        int appointment=r.getInt("appointment_id"); Integer appointmentId=r.wasNull()?null:appointment;
        return new Change(r.getInt("id"),r.getInt("customer_id"),r.getInt("author_id"),appointmentId,ChangeKind.valueOf(r.getString("kind")),r.getString("notes"),
            LocalDate.parse(r.getString("happened_on")),LocalDateTime.parse(r.getString("created_at")),r.getString("source_key"),r.getString("result_key"));
    }
    private void execute(String sql,Object... values) throws DataAccessException {
        db.transaction(c -> {try(var s=c.prepareStatement(sql)) {bind(s,values);s.executeUpdate();}return null;});
    }
    private static void bind(java.sql.PreparedStatement statement,Object... values) throws SQLException {
        for(int i=0;i<values.length;i++) statement.setObject(i+1,values[i]);
    }
}
