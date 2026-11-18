package com.maisonglow.dao;

import com.maisonglow.exception.*;
import com.maisonglow.service.AuthenticationService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

/** Enforces ADMIN on EVERY operation of retained legacy services, including after logout. */
public final class AdminDaoGuard {
    private AdminDaoGuard() { }
    @FunctionalInterface public interface BeforeWrite {void run()throws DataAccessException;}
    public static <T> T protect(Class<T> contract,T delegate,AuthenticationService auth) {
        return protect(contract,delegate,auth,() -> { });
    }
    public static <T> T protect(Class<T> contract,T delegate,AuthenticationService auth,BeforeWrite beforeWrite) {
        return contract.cast(Proxy.newProxyInstance(contract.getClassLoader(),new Class<?>[]{contract},(proxy,method,args) -> {
            if(method.getDeclaringClass()!=Object.class) {
                try {auth.requireAdmin();}
                catch(AuthorizationException e) {throw new AccessDeniedDataException(e.getMessage(),e,auth.currentSession().isEmpty());}
                if(method.getName().equals("create")||method.getName().equals("update")||method.getName().equals("delete"))beforeWrite.run();
            }
            try {return method.invoke(delegate,args);}
            catch(InvocationTargetException e) {throw e.getCause();}
        }));
    }
}
