# Diccionario de datos — Maison Glow

> Fuente: [`sql/schema.sql`](../../sql/schema.sql) (diseño, no aplicado en la Entrega 1). Diagramas y cobertura de RF: [`modelo-entidad-relacion.md`](modelo-entidad-relacion.md).
> Fechas en texto ISO-8601 (`yyyy-MM-dd`, `yyyy-MM-ddTHH:mm:ss`, hora `HH:mm`); booleanos `0/1`; dinero en pesos colombianos.
> **PK** clave primaria · **FK** clave foránea · **UK** única · **NN** no nula.

## Personas y acceso

### `customer` — clientes (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK, autoincremental | Identificador |
| `document` | TEXT | NN, UK | Documento de identidad |
| `name` | TEXT | NN | Nombre completo |
| `phone` | TEXT | NN | Teléfono |
| `email` | TEXT | NN | Correo (destino de la factura) |
| `no_show_count` | INTEGER | NN, ≥ 0, por defecto 0 | Inasistencias acumuladas |
| `registered_at` | TEXT | NN, por defecto hoy | Fecha de registro |

### `professional` — profesionales (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `document` | TEXT | NN, UK | Documento |
| `name`, `phone`, `email` | TEXT | NN | Datos de contacto |
| `specialty` | TEXT | NN | Especialidad |
| `work_start`, `work_end` | TEXT | NN, `work_start < work_end` | Jornada diaria (`HH:mm`) |
| `active` | INTEGER | NN, 0/1, por defecto 1 | Si atiende actualmente |

### `app_user` — usuarios del sistema (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `username` | TEXT | NN, UK | Nombre de usuario |
| `password_hash` | TEXT | NN | Contraseña con hash (nunca en texto plano) |
| `role` | TEXT | NN, `ADMIN` o `CUSTOMER` | Rol |
| `customer_id` | INTEGER | FK → `customer`, UK | Cliente asociado; obligatorio si el rol es `CUSTOMER` y nulo si es `ADMIN` |
| `active` | INTEGER | NN, 0/1 | Si puede ingresar |
| `created_at` | TEXT | NN | Fecha de creación |

## Servicios

### `beauty_service` — servicios (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `name` | TEXT | NN, UK | Nombre |
| `description` | TEXT | NN, por defecto vacío | Descripción |
| `price` | REAL | NN, > 0 | Precio |
| `duration_minutes` | INTEGER | NN, > 0 | Duración, define cuánto bloquea la agenda |
| `active` | INTEGER | NN, 0/1 | Si se ofrece |

### `professional_service` — servicios que ofrece cada profesional (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `professional_id` | INTEGER | PK, FK → `professional`, cascada | Profesional |
| `service_id` | INTEGER | PK, FK → `beauty_service`, cascada | Servicio |

## Inventario

### `product_category` — categorías (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `name` | TEXT | NN, UK | Nombre de la categoría |

### `product` — productos (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `name` | TEXT | NN, UK | Nombre |
| `category_id` | INTEGER | NN, FK → `product_category` | Categoría |
| `price` | REAL | NN, > 0 | Precio de venta |
| `stock` | INTEGER | NN, ≥ 0 | Unidades disponibles (solo cambia mediante `stock_movement`) |
| `minimum_stock` | INTEGER | NN, ≥ 0 | Umbral de alerta de stock bajo |
| `active` | INTEGER | NN, 0/1 | Si se puede vender |

### `stock_movement` — movimientos de inventario (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `product_id` | INTEGER | NN, FK → `product` | Producto afectado |
| `movement_type` | TEXT | NN, `SALE`/`RESTOCK`/`ADJUSTMENT` | Motivo |
| `quantity` | INTEGER | NN, ≠ 0 | Cantidad con signo (negativa al vender) |
| `reason` | TEXT | NN | Nota libre |
| `sale_line_id` | INTEGER | FK → `sale_line` | Línea que originó la salida; obligatoria si el tipo es `SALE` |
| `created_by` | INTEGER | FK → `app_user` | Quién lo registró |
| `created_at` | TEXT | NN | Momento |

## Citas

### `appointment` — citas (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `customer_id` | INTEGER | NN, FK → `customer` | Cliente |
| `professional_id` | INTEGER | NN, FK → `professional` | Profesional |
| `service_id` | INTEGER | NN, FK → `beauty_service` | Servicio |
| `start_at` | TEXT | NN | Inicio |
| `end_at` | TEXT | NN, `> start_at` | Fin (inicio + duración del servicio) |
| `status` | TEXT | NN, `PENDING`/`CONFIRMED`/`COMPLETED`/`CANCELLED`/`NO_SHOW` | Estado (`AppointmentStatus`) |
| `deposit` | REAL | NN, ≥ 0 | Anticipo exigido por la regla |
| `reminder_at` | TEXT | — | Momento programado del recordatorio |
| `created_at` | TEXT | NN | Fecha de creación |

## Ventas, pagos y facturas

### `sale` — ventas (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `customer_id` | INTEGER | NN, FK → `customer` | Comprador |
| `appointment_id` | INTEGER | UK, FK → `appointment` | Cita que originó la venta, si aplica |
| `sold_at` | TEXT | NN | Momento de la venta |
| `total` | REAL | NN, ≥ 0 | Total calculado por `Sale.calculateTotal()` |

### `sale_line` — líneas de venta (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `sale_id` | INTEGER | NN, FK → `sale`, cascada | Venta |
| `product_id` | INTEGER | FK → `product` | Producto vendido (excluyente con `service_id`) |
| `service_id` | INTEGER | FK → `beauty_service` | Servicio vendido (excluyente con `product_id`) |
| `promotion_id` | INTEGER | FK → `promotion`; solo con producto | Promoción aplicada |
| `quantity` | INTEGER | NN, > 0 | Cantidad |
| `unit_price` | REAL | NN, > 0 | Precio unitario **al momento de vender** |

### `payment` — pagos (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `sale_id` | INTEGER | FK → `sale` | Venta pagada (si `kind = SALE_PAYMENT`) |
| `appointment_id` | INTEGER | FK → `appointment` | Cita con anticipo (si `kind = DEPOSIT`) |
| `kind` | TEXT | NN, `DEPOSIT`/`SALE_PAYMENT` | Tipo de pago |
| `method` | TEXT | NN, `CASH`/`CARD`/`TRANSFER` | Medio |
| `amount` | REAL | NN, > 0 | Valor |
| `paid_at` | TEXT | NN | Momento |

### `invoice` — facturas (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `sale_id` | INTEGER | NN, UK, FK → `sale` | Venta facturada (1 a 1) |
| `invoice_number` | TEXT | NN, UK | Número consecutivo |
| `issued_at` | TEXT | NN | Emisión |
| `recipient_email` | TEXT | NN | Correo al que se envía |
| `email_status` | TEXT | NN, `PENDING`/`SENT`/`FAILED` | Estado del envío |
| `sent_at` | TEXT | — | Momento del envío |
| `error_message` | TEXT | — | Motivo si falló |

## Promociones

### `promotion` — promociones (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `name` | TEXT | NN | Nombre |
| `description` | TEXT | NN | Descripción |
| `discount_percentage` | REAL | NN, `> 0` y `≤ 100` | Descuento |
| `valid_from`, `valid_to` | TEXT | NN, `valid_to ≥ valid_from` | Vigencia |
| `origin` | TEXT | NN, `MANUAL`/`LOW_ROTATION` | Cómo se creó |
| `active` | INTEGER | NN, 0/1 | Si está habilitada |

### `promotion_product` — productos de cada promoción (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `promotion_id` | INTEGER | PK, FK → `promotion`, cascada | Promoción |
| `product_id` | INTEGER | PK, FK → `product`, cascada | Producto |

## Notificaciones y configuración

### `notification` — notificaciones (D2)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `type` | TEXT | NN, `LOW_STOCK`/`LOW_ROTATION`/`APPOINTMENT_REMINDER` | Tipo |
| `audience` | TEXT | NN, `ADMIN`/`CUSTOMER` | A quién va dirigida |
| `user_id` | INTEGER | FK → `app_user`, cascada | Destinatario concreto, si lo hay |
| `product_id` | INTEGER | FK → `product`, cascada | Producto relacionado |
| `appointment_id` | INTEGER | FK → `appointment`, cascada | Cita relacionada |
| `message` | TEXT | NN | Texto mostrado |
| `created_at` | TEXT | NN | Creación |
| `read_at` | TEXT | — | Lectura (nulo = sin leer) |

### `app_setting` — configuración y reglas (D1)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `setting_key` | TEXT | PK | Clave |
| `setting_value` | TEXT | NN | Valor |
| `description` | TEXT | NN | Significado |

Claves iniciales (`sql/seed.sql`): `business_name`, `deposit_percentage`, `min_booking_notice_hours`, `reminder_hours_before`, `no_show_penalty_threshold`, `low_rotation_window_days`, `low_rotation_top_n`, `low_rotation_discount_percentage`.

## Inteligencia artificial

### `try_on_session` — probador virtual (AI)
| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | INTEGER | PK | Identificador |
| `customer_id` | INTEGER | NN, FK → `customer` | Cliente |
| `source_image_path` | TEXT | NN | Ruta de la foto original |
| `transformation` | TEXT | NN | Cambio solicitado (p. ej. "rojo cobrizo") |
| `result_image_path` | TEXT | — | Ruta de la simulación generada |
| `status` | TEXT | NN, `PENDING`/`COMPLETED`/`FAILED` | Estado |
| `provider` | TEXT | NN | Implementación usada (simulada o API) |
| `error_message` | TEXT | — | Motivo si falló |
| `created_at` | TEXT | NN | Creación |

### `assistant_conversation`, `assistant_message`, `assistant_tool_call` — asistente (AI)
| Tabla | Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|---|
| `assistant_conversation` | `id` | INTEGER | PK | Identificador |
| | `user_id` | INTEGER | NN, FK → `app_user` | Quién conversa |
| | `started_at` | TEXT | NN | Inicio |
| `assistant_message` | `id` | INTEGER | PK | Identificador |
| | `conversation_id` | INTEGER | NN, FK → `assistant_conversation`, cascada | Conversación |
| | `sender` | TEXT | NN, `USER`/`ASSISTANT` | Emisor |
| | `content` | TEXT | NN | Texto |
| | `created_at` | TEXT | NN | Momento |
| `assistant_tool_call` | `id` | INTEGER | PK | Identificador |
| | `message_id` | INTEGER | NN, FK → `assistant_message`, cascada | Mensaje que originó la llamada |
| | `tool_name` | TEXT | NN | Herramienta de la lista blanca |
| | `arguments` | TEXT | NN | Argumentos en JSON |
| | `result` | TEXT | — | Resultado |
| | `requires_confirmation` | INTEGER | NN, 0/1 | Si modifica datos y exige confirmación |
| | `status` | TEXT | NN, `PENDING_CONFIRMATION`/`EXECUTED`/`REJECTED`/`FAILED` | Estado |
| | `executed_at` | TEXT | — | Ejecución |

## Vistas

| Vista | Qué devuelve | RF |
|---|---|---|
| `v_low_stock` | Productos activos con `stock <= minimum_stock` | 19 |
| `v_product_sales` | Unidades vendidas, ingresos y última venta por producto (incluye los que nunca se vendieron) | 28, 29 |
| `v_service_demand` | Número de citas no canceladas por servicio | 28 |
| `v_customer_stats` | Citas, compras y gasto total por cliente | 18 |

## Triggers

| Trigger | Efecto |
|---|---|
| `trg_appointment_no_overlap_insert` / `_update` | Aborta con `professional_schedule_conflict` si una cita activa se cruza con otra del mismo profesional |
| `trg_stock_movement_apply` | Suma la cantidad del movimiento al `stock` del producto; el `CHECK` aborta si quedaría negativo |
