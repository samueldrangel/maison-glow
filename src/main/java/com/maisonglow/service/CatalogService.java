package com.maisonglow.service;

import com.maisonglow.dao.CommerceDao;
import com.maisonglow.exception.*;
import com.maisonglow.model.Commerce.CatalogProduct;
import com.maisonglow.model.Product;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/** Publication is separate from inventory activity. New and legacy products start unpublished. */
public final class CatalogService {
    private final AuthenticationService auth;
    private final ProductService products;
    private final CommerceDao store;
    private final PhotoStore photos;
    private final TransactionRunner transactions;
    private final Clock clock;
    public CatalogService(AuthenticationService auth,ProductService products,CommerceDao store,PhotoStore photos,TransactionRunner transactions,Clock clock) {
        this.auth=auth;this.products=products;this.store=store;this.photos=photos;this.transactions=transactions;this.clock=clock;
    }
    public List<CatalogProduct> listPublished(String search) throws BusinessRuleException,DataAccessException,ValidationException {
        auth.requireAuthenticated();
        String query=search==null?"":search.trim().toLowerCase(Locale.ROOT);
        return transactions.execute(() -> {
            store.expire(LocalDateTime.now(clock));
            var result=new java.util.ArrayList<CatalogProduct>();
            for(var p:products.listActive()) if(store.listing(p.getId()).published()&&p.getName().toLowerCase(Locale.ROOT).contains(query)) result.add(snapshot(p));
            return List.copyOf(result);
        });
    }
    public CatalogProduct findPublished(int id) throws BusinessRuleException,DataAccessException,ValidationException {
        auth.requireAuthenticated();
        return transactions.execute(() -> {
            store.expire(LocalDateTime.now(clock));Product p=products.findById(id);
            if(!p.isActive()||!store.listing(id).published()) throw new BusinessRuleException("El producto no está publicado.");
            return snapshot(p);
        });
    }
    public void setPublished(int id,boolean published) throws BusinessRuleException,DataAccessException,ValidationException {
        transactions.execute(() -> {auth.requireAdmin();Product p=products.findById(id);
            if(published&&!p.isActive())throw new BusinessRuleException("Activa el producto antes de publicarlo.");
            store.saveListing(id,published,store.listing(id).coverKey());return null;});
    }
    public void setCover(int id,Path source) throws BusinessRuleException,DataAccessException,ValidationException {
        auth.requireAdmin();products.findById(id);
        var login=auth.requireSessionId();
        String key=photos.importPhoto(source);
        try {transactions.execute(() -> auth.inSession(login,() -> {auth.requireAdmin();var old=store.listing(id);store.saveListing(id,old.published(),key);store.enqueuePhotoDelete(old.coverKey());return null;}));}
        catch(BusinessRuleException|ValidationException|DataAccessException|RuntimeException e) {try{photos.delete(key);}catch(DataAccessException cleanup){e.addSuppressed(cleanup);}throw e;}
        photos.cleanPending(store);
    }
    public Path cover(int id) throws BusinessRuleException,DataAccessException {
        var account=auth.requireAuthenticated();Product p=products.findById(id);var listing=store.listing(id);
        if(account.role()!=com.maisonglow.model.UserRole.ADMIN&&(!p.isActive()||!listing.published()))throw new AuthorizationException("Producto no disponible.");
        return photos.resolve(listing.coverKey());
    }
    private CatalogProduct snapshot(Product p)throws DataAccessException {
        var listing=store.listing(p.getId());return new CatalogProduct(p.getId(),p.getName(),p.getCategory().getName(),p.getPrice(),Math.max(0,p.getStock()-store.reserved(p.getId())),listing.published(),listing.coverKey()!=null);
    }
}
