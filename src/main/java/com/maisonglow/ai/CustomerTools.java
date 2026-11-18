package com.maisonglow.ai;

import com.maisonglow.service.*;
import com.maisonglow.exception.*;
import java.util.*;

/** Customer whitelist has no global customer lookup, stock report or global agenda. */
public final class CustomerTools {
    private CustomerTools() { }
    @FunctionalInterface private interface Action {
        String run(Map<String,String> args)throws ValidationException,BusinessRuleException,DataAccessException;
    }
    private record Tool(String getName,String getDescription,Map<String,String> getParameters,
                        boolean requiresConfirmation,Action action) implements AssistantTool {
        public String execute(Map<String,String> parameters)throws ValidationException,BusinessRuleException,DataAccessException {
            if(!getParameters.keySet().containsAll(parameters.keySet()))throw new ValidationException("La herramienta no admite esos parámetros.");
            return action.run(parameters);
        }
    }
    public static List<AssistantTool> create(CustomerPortalService portal,CatalogService catalog) {
        return List.of(
            new Tool("my_profile","Consultar mi perfil",Map.of(),false,p -> portal.profile().getName()),
            new Tool("list_appointments_by_date","Consultar mis citas",Map.of("date","Fecha yyyy-MM-dd"),false,p -> {
                var date=ToolArguments.date(p,"date");
                return portal.myAppointments().stream().filter(a -> a.getStart().toLocalDate().equals(date)).map(Object::toString).collect(java.util.stream.Collectors.joining("\n"));
            }),
            new Tool("published_products","Consultar productos publicados",Map.of("search","Búsqueda opcional"),false,p -> catalog.listPublished(p.getOrDefault("search","")).toString()),
            new Tool("book_appointment","Reservar mi cita sin declarar un pago",Map.of("professionalId","Profesional","treatmentId","Tratamiento","start","Inicio yyyy-MM-ddTHH:mm"),true,p -> {
                var booked=portal.book(ToolArguments.id(p,"professionalId"),ToolArguments.id(p,"treatmentId"),ToolArguments.dateTime(p,"start"));
                return "Cita pendiente ["+booked.getId()+"]. Anticipo requerido: "+portal.requiredDeposit(booked.getId());
            })
        );
    }
}
