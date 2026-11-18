# Roadmap de ampliación — Cliente y administrador

> Plan vigente del 17 de noviembre de 2026. Presentación: 18 de noviembre. **Se conserva SQLite local; nube aplazada y fuera del alcance actual.**
> Alcance: cuentas con roles, autoservicio del cliente, productos publicados con fotos e historial de cambios.
> S-R01 y S-R02 están en curso: [contratos y backend inicial de autenticación](fase3/roles-contratos.md). El resto sigue pendiente; las pantallas y la protección integral aún no están conectadas.
> Base: [Corte 3](roadmap-corte3.md), [instalación y estado actual](../README.md), [esquema](../sql/schema.sql).

## 0. Resumen de seguimiento del equipo

**Estado inicial del alcance nuevo: 0 de 20 tareas terminadas.** Es un seguimiento del plan, no un reporte automático de Git. Actualizar esta tabla y la fila detallada de cada tarea en el mismo PR; registrar su enlace y el bloqueo real, si existe.

| Integrante | Qué entrega | Orden de trabajo | Terminadas / total | Estado inicial | Próximo paso |
|---|---|---|---|---|---|
| **Isabella** | Frontend de acceso y administración | Primero I-R01 → I-R06 | 0 / 6 | En curso: I-R01 e I-R02 hechas y con el servicio real de S-R02 conectado, pendientes de revisión y fusión | Fusionar los PR de I-R01 y I-R02; seguir con I-R03 cuando exista S-R03 |
| **Nicoll** | Frontend del cliente: perfil, tienda, compras, citas, fotos e IA | Después de I-R06: N-R01 → N-R06 | 0 / 6 | En espera del bloque 1 | Revisar contratos y casos de uso; programar al recibir el traspaso |
| **Samuel** | Backend local completo y semilla opcional | S-R01 → S-R07; S-R08 después, si hay tiempo | 0 / 8 | En curso: S-R01 / S-R02 | Revisar contratos y backend de cuentas antes de seguir con catálogo |

**Estados:** Pendiente · En curso · Bloqueada (indicar causa) · Terminada (PR fusionado y aceptación comprobada). La espera prevista de Nicoll no cuenta como trabajo iniciado. Para los porcentajes, usar tareas terminadas / tareas totales; no representa horas consumidas.

| Orden | ID | Tarea resumida | Responsable | Se habilita cuando | Estado | PR / bloqueo |
|---|---|---|---|---|---|---|
| B1 | S-R01 | Contratos, permisos y migraciones | Samuel | Base actual verificada | En curso | [Contratos y migración 1](fase3/roles-contratos.md); revisión pendiente |
| B2 | S-R02 | Cuentas, login y sesión | Samuel | S-R01 | En curso | Backend verificado: 693 pruebas pasan; integración visual y de módulos pendiente |
| B3 | S-R03 | Publicación de productos y archivos | Samuel | S-R02 | Pendiente | — |
| B4 | S-R04 | Pedidos, reservas y ventas | Samuel | S-R03 | Pendiente | — |
| B5 | S-R05 | Perfil y citas propias | Samuel | S-R04 | Pendiente | — |
| B6 | S-R06 | Fotos, historial e IA por cuenta | Samuel | S-R05 | Pendiente | — |
| B8 | S-R08 | Semilla local ampliada (opcional) | Samuel | S-R07 | Pendiente, posterior al cierre | — |
| F1 | I-R01 | Pantallas de acceso | Isabella | S-R01; integrar S-R02 | En curso: pantallas, sesión y pruebas hechas y conectadas al servicio real de cuentas (S-R02, ya en `develop`); falta revisión y fusión | PR #57 y #58 (ramas `feature/roles-i-r01-acceso` y `feature/roles-i-r01-sesion`), sin fusionar |
| F2 | I-R02 | Navegación por rol | Isabella | I-R01 | En curso: navegación y menú por rol, cierre de sesión que descarta lo pendiente y componentes compartidos hechos en local | PR #59 (rama `feature/roles-i-r02`, apilada sobre I-R01); sin fusionar |
| F3 | I-R03 | Productos y fotos del administrador | Isabella | I-R02, S-R03 | En curso: hecho el componente de foto (cámara o archivo, vista previa, repetir, descartar); faltan publicar/retirar y guardar la portada, que esperan a S-R03 | Rama `feature/roles-i-r03`, apilada sobre I-R02; PR sin abrir |
| F4 | I-R04 | Pedidos y ventas del administrador | Isabella | I-R03, S-R04 | Pendiente | — |
| F5 | I-R05 | Agenda, clientes, historial y cuentas | Isabella | I-R04, S-R05, S-R06 | Pendiente | — |
| F6 | I-R06 | Verificación y traspaso | Isabella | I-R05 | Pendiente | — |
| F7 | N-R01 | Inicio y perfil del cliente | Nicoll | I-R06, S-R05 | Pendiente | — |
| F8 | N-R02 | Tienda y detalle de producto | Nicoll | N-R01, S-R03 | Pendiente | — |
| F9 | N-R03 | Carrito y Mis compras | Nicoll | N-R02, S-R04 | Pendiente | — |
| F10 | N-R04 | Reservas y Mis citas | Nicoll | N-R03, S-R05 | Pendiente | — |
| F11 | N-R05 | Fotos, cambios y probador propios | Nicoll | N-R04, S-R06 | Pendiente | — |
| F12 | N-R06 | Revisión integral de frontend | Nicoll | N-R05 | Pendiente | — |
| H1 | S-R07 | Integración local y compatibilidad | Samuel | S-R06, I-R06, N-R06 | Pendiente | — |

B = backend local; F = frontend secuencial; H = cierre local. S-R08 es opcional y no bloquea ninguna pantalla ni la presentación. El backend y un único bloque de frontend pueden avanzar al mismo tiempo. Los números de ambas líneas no implican trabajo paralelo de Isabella y Nicoll.

## 1. Reparto y orden obligatorio

Se conserva el orden del roadmap anterior: **primero Isabella termina todo su bloque de frontend; después empieza Nicoll**. Los nombres registrados en el repositorio son **Nicoll Gómez** e **Isabella Celedón** (Nicola e Isabela en la solicitud).

| Integrante | Responsabilidad nueva | Orden |
|---|---|---|
| **Isabella** | Todo el frontend de acceso, navegación por rol y panel administrador | Bloque 1 completo, I-R01 → I-R06 |
| **Nicoll** | Todo el frontend del área del cliente: perfil, tienda, compras, citas, fotos, historial e IA | Bloque 2 completo, N-R01 → N-R06, después del traspaso de Isabella |
| **Samuel** | Todo el backend local: servicios, permisos, DAO, SQLite, archivos, IA y semilla opcional | S-R01 → S-R07; S-R08 opcional después; puede avanzar mientras se hace frontend |

**No hay frontend paralelo.** Mientras espera su turno, Nicoll puede revisar entregables y preparar casos de uso, pero no abre otra implementación de pantallas, estilos o navegación. Samuel no asume pantallas nuevas: entrega servicios y corrige la integración desde el backend.

La nueva distribución prevalece sobre las asignaciones históricas de backend de `TEAM.md` y `PLANIFICACION.md` para estas tareas. Los trabajos anteriores conservan su autoría.

## 2. Punto de partida comprobado

- Ya existen vistas JavaFX de clientes, productos, ventas, agenda, estadísticas, asistente y probador; se amplían y reutilizan.
- El negocio ya persiste en SQLite y la venta descuenta inventario dentro de una transacción.
- `sql/schema.sql` ya define `app_user`, `ADMIN` y `CUSTOMER`, pero esa tabla por sí sola no implementa registro, login ni autorización. El arranque actual abre Inicio sin autenticación.
- Ya existe captura con cámara y selección de archivo en el probador. Se reutiliza la experiencia, separando las fotos de producto de las fotos privadas del cliente.
- El historial de IA usa archivos de texto; no equivale a una galería permanente de cambios reales. Los resultados del probador pueden ser temporales y deben copiarse si el usuario decide conservarlos.
- El registro actual de un cliente es una ficha del negocio; una cuenta de acceso necesita credenciales y vinculación comprobada a esa ficha.

## 3. Alcance funcional y permisos

| Acción | Cliente | Administrador |
|---|---|---|
| Crear cuenta e iniciar/cerrar sesión | Registro público crea únicamente `CUSTOMER` | Primer administrador mediante configuración inicial controlada |
| Perfil | Consultar y editar sus datos de contacto y contraseña | Gestionar clientes y activar/desactivar cuentas según permisos |
| Productos | Ver publicados, buscar y consultar precio/disponibilidad | Crear, editar, publicar, retirar, ajustar inventario y gestionar fotos |
| Compras | Carrito, confirmar compra y ver sus comprobantes | Registrar ventas, revisar compras y gestionar su estado |
| Citas | Ver horarios disponibles; reservar, reprogramar o cancelar sus citas dentro de las reglas | Administrar agenda completa, confirmar, completar y marcar inasistencias |
| Fotos e historial | Añadir, consultar y retirar sus fotos y notas; comparar antes/después | Acceder a los registros autorizados para la atención y añadir registros del servicio identificados por autor |
| Probador y asistente | Operar sobre su propia cuenta y datos | Operar con el alcance administrativo autorizado |
| Estadísticas y configuración | Solo resumen personal de compras y citas | Indicadores del negocio, profesionales, tratamientos y configuración |

Reglas obligatorias:

1. Ocultar botones mejora la navegación; **cada servicio también comprueba sesión, rol y propiedad del registro**. El cliente no elige el `customerId` con el que se ejecuta una operación.
2. `Professional` sigue siendo una entidad del negocio, sin añadir un tercer rol en esta ampliación.
3. Las cuentas existentes no se reclaman escribiendo un documento o correo conocido. Las fichas antiguas se vinculan mediante un procedimiento verificado por el administrador; nunca se inventan contraseñas para ellas.
4. Cerrar sesión invalida acciones pendientes y limpia carrito, imágenes, chats y vistas conservadas por `Navigator`. Ninguna cuenta hereda el estado de otra.
5. Una cita completada por el administrador puede generar un registro del servicio. Una nota personal del cliente no cambia el estado oficial de una cita.

## 4. Decisiones de alcance para una primera versión

- **Plataforma:** se mantiene JavaFX + SQLite local en `data/session/maison-glow.db`. No se implementan API central, PostgreSQL, alojamiento ni sincronización entre equipos en este alcance. Después de la presentación del 18 de noviembre se evaluará si hace falta compartir datos en línea. No es obligatorio cambiar de motor para esa eventual etapa.
- **Compra:** primera versión con retiro y pago en el local. Confirmar crea un pedido `PENDIENTE_PAGO` y reserva existencias; confirmar pago genera la venta y consume la reserva en una sola transacción. Cancelar o vencer el pedido libera la reserva una sola vez. La UI distingue pedido de comprobante de venta. Pasarela de pago y envíos quedan para una fase posterior.
- **Disponibilidad:** stock disponible = stock físico menos reservas activas. Se valida nuevamente al confirmar; dos carritos no pueden comprometer la última unidad. Un doble clic o reintento no duplica el pedido ni la venta.
- **Fotos:** una portada por producto en el MVP, con cámara o archivo y vista previa. Galería múltiple después. Fotos personales privadas y persistentes, con fecha, descripción, autor y relación opcional con cita; las simulaciones se identifican como tales.
- **Deluxe:** queda pendiente definir si significa membresía, paquete de servicios o beneficios. No se implementa como tercer rol. Si es un plan comercial, se modela separado del rol, con precio, vigencia, beneficios y reglas de cancelación acordados antes de estimarlo.
- **Plazos:** no se fija una fecha de entrega sin conocer disponibilidad. Las estimaciones de la sección 8 son orientativas y empiezan cuando la base anterior compila y los contratos están acordados.

## 5. Preparación y backend — Samuel

Prioridad **P0** = necesaria para cerrar la ampliación; **P1** = mejora posterior. Una tarea solo se marca terminada al fusionarse y cumplir su aceptación.

| ID | Pri. | Entregable | Depende de | Criterio de aceptación | Estado |
|---|---|---|---|---|---|
| S-R01 | P0 | Contratos de servicios, matriz de permisos y evolución de datos | Base actual verificada | Acordar sesión, errores, pedidos, publicación y archivos; migraciones versionadas sobre una copia de una BD existente, sin borrar ni recrear datos; actualizar MER/diccionario. Acordar duración de reserva y reglas de cancelación. | En curso: contratos, MER/diccionario y migración 1 escritos; pendiente revisión |
| S-R02 | P0 | Cuentas, registro, autenticación, sesión y autorización | S-R01 | Cuenta/cliente creados de forma atómica; identificador de acceso normalizado y único; contraseñas con hash adaptativo y sal; primer administrador sin clave fija en Git; cambio de contraseña, desactivación e invalidación de sesión. Pruebas de acceso anónimo, rol incorrecto y datos ajenos. | En curso: backend inicial; pendiente integración y revisión |
| S-R03 | P0 | Catálogo publicable y almacenamiento de imágenes | S-R02 | Separar activo de publicado; cliente solo recibe publicados; guardar portada mediante cámara/archivo; validar contenido, tamaño y dimensiones; rutas gestionadas por la app, sin aceptar rutas arbitrarias. No quedan archivos huérfanos ante fallo ni se pierde la portada anterior. | Pendiente |
| S-R04 | P0 | Pedidos, reservas de stock, compra y consulta propia | S-R03 | Reutilizar venta transaccional existente; precio recalculado por backend; reservar/liberar sin duplicados; vencimientos procesados al arrancar y durante uso; confirmar pago solo como administrador; convertir pedido a venta sin descontar dos veces. Probar competencia por stock, reintentos, cancelación y fallos parciales. | Pendiente |
| S-R05 | P0 | Autoservicio de citas y perfil | S-R04 | Derivar cliente desde sesión; disponibilidad sin revelar agenda ajena; conservar reglas de horario, anticipo y cruces; cliente no confirma pagos ni completa citas; contacto editable sin alterar rol, identidad vinculada o inasistencias. | Pendiente |
| S-R06 | P0 | Historial de cambios, fotos e IA por cuenta | S-R05 | Guardar originales y resultados aceptados en almacenamiento permanente; distinguir cambio real de simulación; permisos por propietario y autorización de atención; retirada de fotos y tratamiento de archivos fallidos. Asistente/herramientas y persistencia de IA filtrados por cuenta; ninguna acción pendiente sobrevive al logout. | Pendiente |
| S-R07 | P0 | Integración local, compatibilidad y cierre de etapa | S-R06, I-R06, N-R06 | Servicios y herramientas sin vías de acceso antiguas que omitan permisos; revisar también consola, consultas y exportaciones. Verificar actualización, reinicio, copias de seguridad y restauración de BD + medios + historial de IA. Actualizar instrucciones y evidencia. La entrega funciona localmente; la nube no es un criterio de cierre. | Pendiente |

Samuel aplica la autorización a las rutas existentes, no solo a los servicios nuevos. La consola no puede convertirse en una vía para saltarse el login. Las comprobaciones del asistente se hacen al ejecutar la herramienta y al confirmar la acción, aunque la conversación ya tuviera una autorización anterior.

Las pruebas de aislamiento incluyen dos clientes distintos, un administrador y una cuenta desactivada. Los datos de IA antiguos sin propietario verificable quedan fuera de la vista de clientes hasta completar una vinculación controlada.

## 6. Frontend, bloque 1 — Isabella

**Una tarea a la vez.** Reutiliza FXML, estilos, componentes y pantallas actuales. Samuel entrega primero los contratos; se permiten datos de prueba explícitos para maquetar mientras llega el backend, pero una tarea no está terminada hasta conectarse al servicio real.

| ID | Pri. | Entregable de frontend | Depende de | Criterio de aceptación | Estado |
|---|---|---|---|---|---|
| I-R01 | P0 | Login, registro, logout y cambio de contraseña | S-R01; integración: S-R02 | Validación por campo, contraseña oculta, errores claros, espera durante petición y registro sin selector de rol. La pantalla inicial depende de la sesión. | En curso (conectada a S-R02; PR #57 y #58 sin fusionar) |
| I-R02 | P0 | Navegación por rol y componentes compartidos | I-R01 | Menú administrativo y estructura del área cliente; acceso directo a vista prohibida rechazado; logout limpia pantallas y solicitudes pendientes. Componentes de carga, errores de archivo/base local y sesión vencida; las operaciones lentas de cámara e IA no bloquean JavaFX. Dejar rutas preparadas para Nicoll. | En curso (PR #59 sin fusionar) |
| I-R03 | P0 | Productos del administrador y portada | I-R02, S-R03 | Crear/editar, publicar/retirar y mostrar portada; Tomar foto/Subir foto, vista previa, repetir/cancelar y sustitución; funciona sin cámara mediante archivo. | En curso (componente de foto hecho; falta S-R03) |
| I-R04 | P0 | Gestión administrativa de pedidos y ventas | I-R03, S-R04 | Estados, detalle y reserva visibles; confirmar pago, cancelar y emitir comprobante de venta; no presentar pedido pendiente como pagado. | Pendiente |
| I-R05 | P0 | Adaptar agenda, clientes, historial y cuentas del administrador | I-R04, S-R05, S-R06 | Agenda conserva operaciones actuales; ficha con historial autorizado y registro de atención; cuentas activables/desactivables sin exponer contraseñas; resumen administrativo y configuración accesibles. | Pendiente |
| I-R06 | P0 | Revisión del bloque y traspaso | I-R05 | Probar flujo administrativo completo y logout/login; entregar componentes, rutas, estados y ejemplos de uso; fusionar todos los cambios antes de que Nicoll abra su bloque. | Pendiente |

Al finalizar I-R06, Isabella entrega una base estable. Los ajustes posteriores sobre archivos compartidos se coordinan con Nicoll, que pasa a ser la única responsable de frontend durante el bloque 2.

## 7. Frontend, bloque 2 — Nicoll

**N-R01 solo empieza después de I-R06 fusionada.** El orden interno también es secuencial.

| ID | Pri. | Entregable de frontend | Depende de | Criterio de aceptación | Estado |
|---|---|---|---|---|---|
| N-R01 | P0 | Inicio del cliente y Mi perfil | I-R06, S-R05 | Saludo, próxima cita, pedidos recientes y acceso a sus datos; estados vacíos útiles; edición de contacto y cambio de contraseña usando lo entregado por Isabella. | Pendiente |
| N-R02 | P0 | Tienda y detalle de producto | N-R01, S-R03 | Buscar/filtrar publicados, portada, precio y disponibilidad; no mostrar ajustes internos de inventario; producto retirado deja de poder comprarse. | Pendiente |
| N-R03 | P0 | Carrito, confirmación y Mis compras | N-R02, S-R04 | Cantidades y total, aviso de retiro/pago local, confirmación única, pedido persistente y detalle propio; explicar falta de stock o cambio de precio antes de continuar. | Pendiente |
| N-R04 | P0 | Reservar y Mis citas | N-R03, S-R05 | Elegir tratamiento/profesional/horario disponible; ver anticipo y condiciones; confirmar, reprogramar o cancelar según reglas; sin selector de otros clientes. | Pendiente |
| N-R05 | P0 | Mis cambios, fotos y probador/asistente propios | N-R04, S-R06 | Línea de tiempo con fecha y notas; cámara/archivo, antes/después y retirada; guardar una simulación explícitamente; sin selector global de clientes ni acceso a chats ajenos. | Pendiente |
| N-R06 | P0 | Revisión integral de frontend y cierre del bloque | N-R05 | Recorrido de registro → compra → cita → foto → reinicio → logout; repetir con otra cuenta; teclado, errores, carga, errores de persistencia local y sesión vencida revisados con dobles de servicio; formularios y listas vacías; corregir regresiones del frontend administrativo. | Pendiente |

## 8. Hitos y estimación orientativa

Estas son jornadas de trabajo efectivo, no fechas comprometidas. Se revisan tras S-R01; cualquier espera por dependencias mueve el calendario.

| Hito | Estimación | Condición para avanzar |
|---|---|---|
| H0: contratos y alcance | Samuel: 1–2 jornadas con revisión del equipo | S-R01 lista; reglas de pedido/anticipo acordadas |
| H1: backend funcional | Samuel: 12–18 jornadas adicionales para S-R02–S-R06 | Cada servicio integrado antes del cierre de la pantalla que lo consume |
| H2: administrador completo | Isabella: 8–12 jornadas después de H0, más esperas por servicios | I-R06 fusionada; servicios administrativos reales disponibles |
| H3: cliente completo | Nicoll: 8–12 jornadas después de H2 | N-R06 fusionada; sin frontend paralelo |
| H4: cierre local | Samuel: 2–3 jornadas con revisión del equipo | S-R07 lista y recorrido de aceptación aprobado |
| H5: semilla ampliada opcional | Samuel: 1–2 jornadas posteriores al cierre | S-R08 lista; no bloquea la presentación |

**Carga secuencial de frontend: 16–24 jornadas**, más preparación, integración y esperas. No se reutiliza automáticamente la promesa anterior de 14 días: hay nuevos flujos y reglas de datos.

**La presentación es mañana, 18 de noviembre. Estas estimaciones describen el alcance completo y no prometen terminarlo en un día.** Primero integrar un recorrido local de acceso por rol, producto publicado, compra y reserva. Si no alcanza el tiempo, documentar los pendientes reales; recuperación de cuenta, Deluxe, semilla grande y nube no desplazan ese recorrido. No dar por finalizado un bloque incompleto para adelantar el de Nicoll.

Cada tarea usa una rama propia, por ejemplo `codex/roles-s-r02` o `codex/roles-i-r03`, desde la base de integración acordada por el equipo. PR hacia `develop`; Samuel revisa frontend e Isabella o Nicoll revisan backend. No cambiar estados históricos para aparentar que estas tareas ya estaban cubiertas.

## 9. Qué mejorar además del registro básico

Las mejoras P0 ya están incluidas arriba; las P1 no bloquean esta primera ampliación.

| Prioridad | Mejora | Resultado y responsables |
|---|---|---|
| P0 | Diferenciar ficha de cliente y cuenta; proteger credenciales y sesión | Registro usable sin duplicar identidades; backend Samuel, pantallas Isabella |
| P0 | Permisos por propiedad, también en IA y consola | Cada cliente accede únicamente a sus operaciones; Samuel, verificación visual Nicoll |
| P0 | Fotos persistentes y publicación controlada | Catálogo con contenido real e historial que sobrevive al reinicio; Samuel + frontend de cada bloque |
| P0 | Estados claros de pedido, cita y pago | No confundir reservar, pagar y completar; Samuel + Isabella/Nicoll en secuencia |
| P0 | Copias que incluyan medios y recuperación comprobada | No perder fotos al respaldar solo SQLite; Samuel documenta y verifica |
| P1 | Recuperación de contraseña y verificación de contacto | Samuel define canal e identidad; Isabella implementa pantallas en un bloque posterior. No simular un correo enviado |
| P1 | Recordatorios y notificaciones reales | Samuel implementa entrega, reintentos y estado; Nicoll muestra preferencias y avisos en un bloque posterior |
| P1 | Deluxe, favoritos y fidelización | Definir primero beneficios y reglas; Samuel backend, Nicoll frontend después de cerrar el MVP |
| P1 | Auditoría de cambios administrativos | Samuel registra actor, fecha y operación sin contraseñas ni imágenes en logs; Isabella añade consulta posterior |
| P1 | Frontend móvil/web, pagos en línea y envíos | Nueva fase con arquitectura, proveedor y operación definidos; estimación separada |

Las mejoras posteriores de frontend también se programan en bloques consecutivos, sin abrir dos frentes a la vez.

## 10. Aceptación final de la ampliación

1. Una instalación existente se actualiza sin perder clientes, citas, ventas, configuración o medios. La copia de seguridad se restaura y vuelve a abrir.
2. Un registro crea solo una cuenta cliente; una ficha antigua no puede apropiarse sin verificación. El primer administrador se configura sin credenciales públicas.
3. El administrador publica un producto con foto y el cliente lo encuentra. Retirarlo impide nuevas compras y conserva el historial.
4. Dos clientes intentan comprar la última unidad: solo uno obtiene reserva. Repetir la confirmación no duplica operaciones; cancelar/vencer libera una vez; cobrar genera una sola venta.
5. El cliente reserva, reprograma y cancela respetando reglas. Intentar una cita cruzada falla sin modificar datos.
6. Fotos, notas y simulaciones guardadas se recargan tras reiniciar. La simulación se distingue de una atención real y una foto cancelada no crea un registro.
7. Cliente A no puede leer ni modificar pedidos, citas, fotos, conversaciones o exportaciones de B mediante servicios, herramientas o navegación. Logout y desactivación invalidan la sesión y las acciones pendientes.
8. El administrador conserva la gestión actual de profesionales, tratamientos, agenda, inventario y estadísticas. No hay regresiones de los flujos previos.
9. `mvn clean verify` pasa; quedan documentadas las pruebas manuales JavaFX y sus resultados. Ninguna clave, BD o foto personal se añade al repositorio.
10. README, MER, diccionario y responsabilidades reflejan lo realmente entregado. Los pendientes P1 continúan separados del cierre P0.

## 11. Datos locales, semilla y eventual nube

### 11.1 Acuerdo para trabajar y presentar

- Cada equipo ejecuta JavaFX con su propia base SQLite local. No hay sincronización automática y los cambios de un computador no aparecen en los demás.
- El repositorio comparte código, esquema y generador de datos ficticios; no se suben bases, fotos personales ni claves.
- Se presenta desde un computador principal. Preparar y verificar allí los datos del recorrido antes de la demostración; cerrar la aplicación y respaldar toda `data/session/`, incluidos medios e historial.
- Para partir de ejemplos equivalentes en distintos equipos se podrá ejecutar la misma semilla de prueba. Eso crea copias independientes y no una base compartida. La semilla debe ser explícita y no sobrescribir trabajo existente.
- No abrir la misma base simultáneamente desde una carpeta sincronizada o unidad de red.

### 11.2 Semilla ampliada local — Samuel, opcional

| ID | Pri. | Tarea | Depende de | Criterio de aceptación | Estado |
|---|---|---|---|---|---|
| S-R08 | P1 | Generador de semilla local reproducible | S-R07 | Perfil pequeño para demo y grande para volumen; fecha base y semilla aleatoria fijas; solo sobre destino de pruebas explícito, nunca al arranque normal; segunda ejecución sin duplicados; relaciones, totales, stock y agenda coherentes; informe de conteos. No sobrescribir datos existentes ni usar información personal real. | Pendiente |

Para mañana basta un conjunto pequeño que cubra dos clientes con cuentas distintas, un administrador, productos publicados/retirados, stock disponible/agotado, tratamientos, profesionales y citas en estados válidos. Las credenciales de prueba se entregan fuera de Git. La semilla actual sigue como está hasta implementar S-R08; este documento no carga datos.

El perfil grande queda para después del cierre: hasta 1.000 clientes, 200 productos, 20 profesionales, 30 tratamientos, 5.000 citas, 3.000 ventas, 1.000 pedidos y 1.000 registros de historial. Ajustar el volumen a las pruebas necesarias; no generar cientos de imágenes mediante API ni enviar correos reales para poblar ejemplos. Debe incluir también cuentas sin actividad y listas vacías.

### 11.3 Nube aplazada

Después del 18 de noviembre, si el equipo confirma que necesita datos compartidos, se estima una etapa separada. Se puede evaluar un backend central que conserve SQLite en el servidor; PostgreSQL será una alternativa si las necesidades lo justifican. No se fija proveedor ni se programan tareas de despliegue ahora.

La tabla inicial cuenta únicamente las 19 tareas del alcance local y S-R08 opcional: **20 tareas planificadas; la nube no forma parte del conteo ni de las dependencias**.

## 12. Cómo empezar y hacer el traspaso

1. **Samuel:** cerrar S-R01 antes de integrar login: acordar sesión, operaciones permitidas y errores. Mantener el armado de servicios en `AppContext`. El dominio y SQLite quedan a su cargo.
2. **Isabella:** empezar I-R01 con las pantallas de acceso y sus estados visuales sobre componentes existentes; puede maquetar con dobles de servicio mientras Samuel entrega S-R02. No crear DAO ni reglas de negocio dentro de controladores. Una tarea solo se marca terminada tras conectar y verificar el servicio real.
3. **Nicoll:** mientras espera I-R06, revisar contratos y preparar el recorrido de cliente y casos de prueba; no programar pantallas en paralelo. Después del traspaso, ejecutar N-R01 → N-R06 con la base ya fusionada.
4. Usar la base `develop` actualizada, que ya incorpora `feature/probar-nicoll`. Una rama por tarea y PR hacia `develop`; no mezclar archivos locales de datos, fotos o configuración privada.
5. Actualizar estado, PR y bloqueos en la tabla inicial. Para la presentación, mostrar lo comprobado y declarar lo que continúe pendiente.
