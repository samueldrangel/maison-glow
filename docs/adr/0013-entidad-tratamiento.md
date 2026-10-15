# ADR-0013: La entidad `Servicio` se llama `Treatment` (Tratamiento)

- **Estado:** Aceptado
- **Fecha:** 2026-10-14
- **Decide:** Samuel (líder), con aviso a Isabella (I-01) y Nicoll (N-03, N-04)

## Contexto
En el proyecto el sufijo `Service` / `Servicio` identifica a las **clases de la capa de negocio** (`CustomerService`, `AppointmentService`, `SettingsService`). La entidad del modelo que representa un corte, una manicura, etc. se llamaba `BeautyService`, así que también terminaba en `Service` sin serlo:

- Leer `BeautyService` hacía pensar en lógica de negocio, no en un dato del catálogo.
- El diseño tuvo que crear una excepción (`BeautyServiceCatalog` / `CatalogoServicios`) para no llegar a `BeautyServiceService`.
- La tabla puente `professional_service` se confundía con la clase de negocio `ProfessionalService`.

Aún no existían el DAO, el catálogo ni `Appointment`, por lo que el cambio era barato.

## Decisión
1. La entidad se llama **`Treatment`** (en los documentos en español, **`Tratamiento`**).
2. Su lógica de negocio sigue la regla general: **`TreatmentService`** (`TratamientoServicio`); se elimina la excepción `BeautyServiceCatalog`. El DAO es `TreatmentDao` / `TreatmentDaoText`.
3. En `Professional`: `addTreatment`, `removeTreatment`, `getTreatments`; `offers(Treatment)` se mantiene.
4. En `sql/schema.sql`: `beauty_service` → `treatment`, `professional_service` → `professional_treatment`, columnas `service_id` → `treatment_id`, vista `v_service_demand` → `v_treatment_demand`. Archivos de texto: `treatments.txt` y `professional_treatments.txt`.
5. Los textos que ve el usuario pueden seguir diciendo "servicio": es la palabra del negocio. La regla aplica solo a identificadores.

## Alternativas consideradas
- **Dejar `BeautyService`:** mantiene la ambigüedad y la excepción de nombres.
- **`SalonOffering`:** genérico y largo; no es un término del dominio.
- **`Procedure`:** suena clínico y se confunde con un procedimiento almacenado.

## Consecuencias
- Positivas: `Service` vuelve a significar solo capa de negocio; todas las clases de negocio siguen `XxxService` sin excepciones.
- Negativas / costos: quien tenga ramas abiertas que usen `BeautyService` debe actualizarlas; los nombres de ramas ya fusionadas (`feature/beauty-service-model`) quedan con el nombre antiguo en el historial.
