package com.maisonglow.service;

import com.maisonglow.dao.*;
import com.maisonglow.exception.*;
import com.maisonglow.model.*;
import com.maisonglow.model.Commerce.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/** Local pickup/pay-at-salon orders. Reservation, payment and stock are one SQLite transaction. */
public final class OrderService {
    private final AuthenticationService auth;
    private final CommerceDao store;
    private final ProductService products;
    private final CustomerService customers;
    private final SaleService sales;
    private final SaleDao saleDao;
    private final TransactionRunner transactions;
    private final Clock clock;
    public OrderService(AuthenticationService auth,CommerceDao store,ProductService products,CustomerService customers,
                        SaleService sales,SaleDao saleDao,TransactionRunner transactions,Clock clock) {
        this.auth=auth;this.store=store;this.products=products;this.customers=customers;this.sales=sales;this.saleDao=saleDao;this.transactions=transactions;this.clock=clock;
    }
    /** The caller supplies quantities, never prices or a customer id. Reuse the same key for retries. */
    public Order place(String requestKey,Map<Integer,Integer> quantities) throws ValidationException,BusinessRuleException,DataAccessException {
        int customer=auth.currentCustomerId();
        if(requestKey==null||!requestKey.matches("[a-zA-Z0-9_-]{8,100}"))throw new ValidationException("Usa una clave de pedido única de 8 a 100 caracteres.");
        if(quantities==null||quantities.isEmpty()||quantities.size()>100)throw new ValidationException("El carrito debe tener entre 1 y 100 productos.");
        Map<Integer,Integer> requested=new TreeMap<>();
        for(var entry:quantities.entrySet()) {
            if(entry.getKey()==null||entry.getKey()<1||entry.getValue()==null||entry.getValue()<1||entry.getValue()>10000)throw new ValidationException("Revisa los productos y cantidades del carrito.");
            requested.put(entry.getKey(),entry.getValue());
        }
        return transactions.execute(() -> {
            if(auth.currentCustomerId()!=customer)throw new AuthorizationException("La sesión cambió. Repite la operación.");
            expire();
            var prior=store.byRequest(customer,requestKey);
            if(prior.isPresent()) {
                var existing=new TreeMap<Integer,Integer>();for(var line:prior.get().lines())existing.put(line.productId(),line.quantity());
                if(!existing.equals(requested))throw new BusinessRuleException("Esta clave ya corresponde a otro carrito.");
                return prior.get();
            }
            List<OrderLine> lines=new ArrayList<>();BigDecimal total=BigDecimal.ZERO;
            for(var entry:requested.entrySet()) {
                Product product=products.findById(entry.getKey());
                if(!product.isActive()||!store.listing(product.getId()).published())throw new BusinessRuleException("El producto ya no está disponible.");
                if(product.getStock()-store.reserved(product.getId())<entry.getValue())throw new BusinessRuleException("Stock insuficiente de "+product.getName()+".");
                lines.add(new OrderLine(product.getId(),product.getName(),entry.getValue(),product.getPrice()));
                total=total.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
            }
            LocalDateTime now=LocalDateTime.now(clock);
            return store.insertOrder(customer,requestKey,now,now.plusMinutes(30),total,lines);
        });
    }
    public List<Order> mine() throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {int customer=auth.currentCustomerId();expire();return store.orders(customer);});
    }
    public List<Order> all() throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {auth.requireAdmin();expire();return store.orders(null);});
    }
    public Order find(int id)throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {auth.requireAuthenticated();expire();Order order=load(id);auth.requireCustomerAccess(order.customerId());return order;});
    }
    public Order cancel(int id)throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {
            auth.requireAuthenticated();expire();Order order=load(id);auth.requireCustomerAccess(order.customerId());
            if(order.status()==OrderStatus.CANCELLED||order.status()==OrderStatus.EXPIRED)return order;
            if(order.status()!=OrderStatus.PENDING_PAYMENT)throw new BusinessRuleException("No se puede cancelar un pedido pagado.");
            store.orderState(id,OrderStatus.CANCELLED,null);return load(id);
        });
    }
    /** A customer cannot claim to have paid; only an administrator can commit the payment. */
    public Order confirmPayment(int id,String method)throws ValidationException,BusinessRuleException,DataAccessException {
        auth.requireAdmin();
        if(!Set.of("CASH","CARD","TRANSFER").contains(method==null?"":method))throw new ValidationException("Selecciona un método de pago válido.");
        return transactions.execute(() -> {
            auth.requireAdmin();expire();Order order=load(id);
            if(order.status()==OrderStatus.PAID)return order;
            if(order.status()!=OrderStatus.PENDING_PAYMENT)throw new BusinessRuleException("El pedido ya no está pendiente de pago.");
            List<SaleLine> lines=new ArrayList<>();
            for(var line:order.lines())lines.add(new SaleLine(products.findById(line.productId()),line.quantity(),line.unitPrice()));
            Sale sale=SaleService.restoreSale(0,customers.findById(order.customerId()),LocalDateTime.now(clock),lines);
            // Release this reservation before stock is consumed; the transaction still protects others.
            store.orderState(id,OrderStatus.PAID,null);
            Sale stored=sales.register(sale);
            store.recordPayment(stored.getId(),method,stored.getTotal(),LocalDateTime.now(clock));
            store.orderState(id,OrderStatus.PAID,stored.getId());return load(id);
        });
    }
    public Sale receipt(int orderId)throws ValidationException,BusinessRuleException,DataAccessException {
        return transactions.execute(() -> {
            Order order=find(orderId);
            if(order.saleId()==null)throw new BusinessRuleException("El pedido aún no tiene comprobante de venta.");
            return saleDao.findById(order.saleId()).orElseThrow(() -> new BusinessRuleException("No se encontró el comprobante."));
        });
    }
    private void expire()throws DataAccessException {store.expire(LocalDateTime.now(clock));}
    private Order load(int id)throws BusinessRuleException,DataAccessException {
        return store.order(id).orElseThrow(() -> new BusinessRuleException("El pedido no existe."));
    }
}
