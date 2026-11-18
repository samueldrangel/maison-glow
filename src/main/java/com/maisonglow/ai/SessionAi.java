package com.maisonglow.ai;

import com.maisonglow.exception.*;
import com.maisonglow.model.UserRole;
import com.maisonglow.service.AuthenticationService;
import java.util.UUID;
import java.util.function.Supplier;

/** Rebuilds providers/confirmation registries for each login and rejects late results after account changes. */
public final class SessionAi {
    private final AuthenticationService auth;
    private final Supplier<AiModule> adminFactory,customerFactory;
    private final java.util.function.BooleanSupplier configuredSimulated;
    private UUID login;
    private AiModule provider;
    public SessionAi(AuthenticationService auth,Supplier<AiModule> adminFactory,Supplier<AiModule> customerFactory) {
        this(auth,adminFactory,customerFactory,() -> true);
    }
    /**
     * @param configuredSimulated what the configuration says (true when no API provider is configured): it answers the
     *        mode while no provider has been built for the session yet, because the provider is built lazily, on the
     *        first call, and asking its mode from the render thread must not probe the network
     */
    public SessionAi(AuthenticationService auth,Supplier<AiModule> adminFactory,Supplier<AiModule> customerFactory,
                     java.util.function.BooleanSupplier configuredSimulated) {
        this.auth=auth;this.adminFactory=adminFactory;this.customerFactory=customerFactory;this.configuredSimulated=configuredSimulated;
    }
    private synchronized AiModule provider()throws BusinessRuleException,DataAccessException {
        UUID current=auth.requireSessionId();
        if(!current.equals(login)) {provider=(auth.requireAuthenticated().role()==UserRole.ADMIN?adminFactory:customerFactory).get();login=current;}
        return provider;
    }
    public AiModule module() {
        SmartAssistant assistant=new SmartAssistant() {
            public AssistantReply reply(String message){return answer(a -> a.reply(message));}
            public AssistantReply confirm(PendingAction action){return answer(a -> a.confirm(action));}
            public AssistantReply reject(PendingAction action){return answer(a -> a.reject(action));}
            private AssistantReply answer(java.util.function.Function<SmartAssistant,AssistantReply> call) {
                try {
                    UUID start=auth.requireSessionId();AssistantReply result=call.apply(provider().assistant());
                    return start.equals(auth.requireSessionId())?result:new AssistantReply("La sesión cambió. Repite la operación.");
                }catch(BusinessRuleException|DataAccessException e){return new AssistantReply("Inicia sesión con una cuenta autorizada para continuar.");}
            }
        };
        return new AiModule(assistant,request -> {
            try {
                UUID start=auth.requireSessionId();
                if(request==null||request.customerId()<1)throw new ValidationException("Selecciona un cliente registrado.");
                auth.requireCustomerAccess(request.customerId());
                TryOnResult result=provider().tryOn().generate(request);
                if(!start.equals(auth.requireSessionId()))throw new ValidationException("La sesión cambió durante la generación.");
                return result;
            }catch(BusinessRuleException e){throw new ValidationException(e.getMessage(),e);}
        },this::isSimulated);
    }
    /**
     * Does not perform a connectivity probe from a render-thread mode query. Before the session has built its provider
     * the mode is what the configuration asks for; after the first call it is the mode the provider really took (an
     * API provider that could not be reached falls back to simulation).
     */
    private synchronized boolean isSimulated() {
        try{return provider==null||!auth.requireSessionId().equals(login)?configuredSimulated.getAsBoolean():provider.isSimulated();}
        catch(BusinessRuleException|DataAccessException e){return configuredSimulated.getAsBoolean();}
    }
    public static AssistantTool bind(AssistantTool delegate,AuthenticationService auth) {
        final UUID login;
        try {login=auth.requireSessionId();}
        catch(BusinessRuleException|DataAccessException e){throw new IllegalStateException("Se requiere una sesión para crear las herramientas.",e);}
        return new AssistantTool() {
            public String getName(){return delegate.getName();}
            public String getDescription(){return delegate.getDescription();}
            public java.util.Map<String,String> getParameters(){return delegate.getParameters();}
            public boolean requiresConfirmation(){return delegate.requiresConfirmation();}
            public String execute(java.util.Map<String,String> args)throws ValidationException,BusinessRuleException,DataAccessException {
                return auth.inSession(login,() -> delegate.execute(args));
            }
        };
    }
}
