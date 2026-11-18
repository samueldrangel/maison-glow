package com.maisonglow;

import com.maisonglow.ai.AiConfig;
import com.maisonglow.dao.AccountDaoSqlite;
import com.maisonglow.model.UserRole;
import com.maisonglow.service.auth.PasswordHasher;
import java.nio.file.Path;
import java.time.Clock;

/** Explicit administrator fixture for legacy UI/business tests. Production has no automatic login. */
public final class TestAccounts {
    private static final char[] PASSWORD="test-only-administrator".toCharArray();
    private static final String HASH=hash();
    private TestAccounts() { }
    private static String hash() {try{return new PasswordHasher().hash(PASSWORD);}catch(Exception e){throw new AssertionError(e);}}
    public static AppContext open(Path folder){return open(folder,Clock.systemDefaultZone(),AiConfig.simulated());}
    public static AppContext open(Path folder,Clock clock,AiConfig config) {
        AppContext context=new AppContext(folder,clock,config);
        try {
            var dao=new AccountDaoSqlite(context.database());
            if(dao.findByUsername("fixture-admin").isEmpty())dao.create("fixture-admin",HASH,UserRole.ADMIN,null);
            context.auth().login("fixture-admin",PASSWORD);
            return context;
        }catch(Exception e){throw new AssertionError("No se pudo preparar el administrador de prueba.",e);}
    }
}
