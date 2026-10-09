-- Maison Glow - complete database schema (SQLite)
--
-- DESIGN DOCUMENT: this script is not applied in delivery 1, which persists in text files
-- (docs/adr/0012). It is validated against SQLite and connected in delivery 2.
--
-- Conventions
--   * Dates and times are ISO-8601 text: date 'yyyy-MM-dd', date-time 'yyyy-MM-ddTHH:mm:ss', time 'HH:mm'.
--   * Booleans are INTEGER 0 (false) or 1 (true).
--   * Money is REAL expressed in Colombian pesos.
--   * Stock is only changed by inserting rows in stock_movement; a trigger applies them to product.stock.
--   * Delivery in which each table is first used: [D1] business logic (in-memory DAO equivalent),
--     [D2] JDBC persistence, [AI] artificial intelligence modules.

PRAGMA foreign_keys = ON;

-- =====================================================================
-- People and access
-- =====================================================================

-- [D1] Customers of the business (RF-01, RF-02).
CREATE TABLE IF NOT EXISTS customer (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    document       TEXT    NOT NULL UNIQUE,
    name           TEXT    NOT NULL,
    phone          TEXT    NOT NULL,
    email          TEXT    NOT NULL,
    no_show_count  INTEGER NOT NULL DEFAULT 0 CHECK (no_show_count >= 0),
    registered_at  TEXT    NOT NULL DEFAULT (date('now'))
);

-- [D1] Professionals who provide services (RF-08).
CREATE TABLE IF NOT EXISTS professional (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    document    TEXT NOT NULL UNIQUE,
    name        TEXT NOT NULL,
    phone       TEXT NOT NULL,
    email       TEXT NOT NULL,
    specialty   TEXT NOT NULL,
    work_start  TEXT NOT NULL,
    work_end    TEXT NOT NULL,
    active      INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    CHECK (work_start < work_end)
);

-- [D2] System users with a role: administrators see the whole business,
-- customers see only their own statistics (RF-18, RF-20, RF-28).
CREATE TABLE IF NOT EXISTS app_user (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    username       TEXT    NOT NULL UNIQUE,
    password_hash  TEXT    NOT NULL,
    role           TEXT    NOT NULL CHECK (role IN ('ADMIN', 'CUSTOMER')),
    customer_id    INTEGER UNIQUE REFERENCES customer (id) ON DELETE CASCADE,
    active         INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    created_at     TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    CHECK ((role = 'ADMIN' AND customer_id IS NULL) OR (role = 'CUSTOMER' AND customer_id IS NOT NULL))
);

-- =====================================================================
-- Services
-- =====================================================================

-- [D1] Beauty services offered (RF-06, RF-07).
CREATE TABLE IF NOT EXISTS beauty_service (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    name              TEXT    NOT NULL UNIQUE,
    description       TEXT    NOT NULL DEFAULT '',
    price             REAL    NOT NULL CHECK (price > 0),
    duration_minutes  INTEGER NOT NULL CHECK (duration_minutes > 0),
    active            INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

-- [D1] Which professional offers which service (RF-09).
CREATE TABLE IF NOT EXISTS professional_service (
    professional_id  INTEGER NOT NULL REFERENCES professional (id) ON DELETE CASCADE,
    service_id       INTEGER NOT NULL REFERENCES beauty_service (id) ON DELETE CASCADE,
    PRIMARY KEY (professional_id, service_id)
);

-- =====================================================================
-- Inventory
-- =====================================================================

-- [D1] Product categories.
CREATE TABLE IF NOT EXISTS product_category (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT NOT NULL UNIQUE
);

-- [D1] Products for sale with their stock (RF-03, RF-04, RF-05, RF-19).
CREATE TABLE IF NOT EXISTS product (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT    NOT NULL UNIQUE,
    category_id    INTEGER NOT NULL REFERENCES product_category (id),
    price          REAL    NOT NULL CHECK (price > 0),
    stock          INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    minimum_stock  INTEGER NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    active         INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1))
);

-- =====================================================================
-- Appointments
-- =====================================================================

-- [D1] Appointments between a customer and a professional for a service (RF-10 to RF-13).
-- end_at is derived (start_at + service duration) and stored so overlaps can be checked in SQL.
CREATE TABLE IF NOT EXISTS appointment (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id      INTEGER NOT NULL REFERENCES customer (id),
    professional_id  INTEGER NOT NULL REFERENCES professional (id),
    service_id       INTEGER NOT NULL REFERENCES beauty_service (id),
    start_at         TEXT    NOT NULL,
    end_at           TEXT    NOT NULL,
    status           TEXT    NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    deposit          REAL    NOT NULL DEFAULT 0 CHECK (deposit >= 0),
    reminder_at      TEXT,
    created_at       TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    CHECK (end_at > start_at)
);

-- =====================================================================
-- Sales, payments and invoices
-- =====================================================================

-- [D1] A sale made to a customer (RF-14). appointment_id links a service sale to its appointment.
CREATE TABLE IF NOT EXISTS sale (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id     INTEGER NOT NULL REFERENCES customer (id),
    appointment_id  INTEGER UNIQUE REFERENCES appointment (id),
    sold_at         TEXT    NOT NULL,
    total           REAL    NOT NULL CHECK (total >= 0)
);

-- [D2] Promotions that can be applied to products (RF-31). origin says how it was created.
CREATE TABLE IF NOT EXISTS promotion (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    name                 TEXT    NOT NULL,
    description          TEXT    NOT NULL DEFAULT '',
    discount_percentage  REAL    NOT NULL CHECK (discount_percentage > 0 AND discount_percentage <= 100),
    valid_from           TEXT    NOT NULL,
    valid_to             TEXT    NOT NULL,
    origin               TEXT    NOT NULL DEFAULT 'MANUAL' CHECK (origin IN ('MANUAL', 'LOW_ROTATION')),
    active               INTEGER NOT NULL DEFAULT 1 CHECK (active IN (0, 1)),
    CHECK (valid_to >= valid_from)
);

-- [D2] Products covered by each promotion.
CREATE TABLE IF NOT EXISTS promotion_product (
    promotion_id  INTEGER NOT NULL REFERENCES promotion (id) ON DELETE CASCADE,
    product_id    INTEGER NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    PRIMARY KEY (promotion_id, product_id)
);

-- [D1] Lines of a sale (RF-15, RF-16). Each line sells exactly one product OR one service:
-- this is how the Sellable interface of the model is stored. unit_price is the price at sale time.
CREATE TABLE IF NOT EXISTS sale_line (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id       INTEGER NOT NULL REFERENCES sale (id) ON DELETE CASCADE,
    product_id    INTEGER REFERENCES product (id),
    service_id    INTEGER REFERENCES beauty_service (id),
    promotion_id  INTEGER REFERENCES promotion (id),
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    unit_price    REAL    NOT NULL CHECK (unit_price > 0),
    CHECK ((product_id IS NULL) <> (service_id IS NULL)),
    CHECK (promotion_id IS NULL OR product_id IS NOT NULL)
);

-- [D2] Every stock change with its reason; quantity is signed (negative for sales) (RF-05, RF-17).
CREATE TABLE IF NOT EXISTS stock_movement (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id     INTEGER NOT NULL REFERENCES product (id),
    movement_type  TEXT    NOT NULL CHECK (movement_type IN ('SALE', 'RESTOCK', 'ADJUSTMENT')),
    quantity       INTEGER NOT NULL CHECK (quantity <> 0),
    reason         TEXT    NOT NULL DEFAULT '',
    sale_line_id   INTEGER REFERENCES sale_line (id),
    created_by     INTEGER REFERENCES app_user (id),
    created_at     TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    CHECK (movement_type <> 'SALE' OR (quantity < 0 AND sale_line_id IS NOT NULL))
);

-- [D2] Money received: appointment deposits and sale payments (RF-26).
CREATE TABLE IF NOT EXISTS payment (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id         INTEGER REFERENCES sale (id),
    appointment_id  INTEGER REFERENCES appointment (id),
    kind            TEXT    NOT NULL CHECK (kind IN ('DEPOSIT', 'SALE_PAYMENT')),
    method          TEXT    NOT NULL CHECK (method IN ('CASH', 'CARD', 'TRANSFER')),
    amount          REAL    NOT NULL CHECK (amount > 0),
    paid_at         TEXT    NOT NULL,
    CHECK ((kind = 'DEPOSIT' AND appointment_id IS NOT NULL)
        OR (kind = 'SALE_PAYMENT' AND sale_id IS NOT NULL))
);

-- [D2] Invoice of a sale and the status of its delivery by email (RF-27).
CREATE TABLE IF NOT EXISTS invoice (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    sale_id          INTEGER NOT NULL UNIQUE REFERENCES sale (id),
    invoice_number   TEXT    NOT NULL UNIQUE,
    issued_at        TEXT    NOT NULL,
    recipient_email  TEXT    NOT NULL,
    email_status     TEXT    NOT NULL DEFAULT 'PENDING' CHECK (email_status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at          TEXT,
    error_message    TEXT
);

-- =====================================================================
-- Notifications and configuration
-- =====================================================================

-- [D2] Alerts: low stock, low rotation for the administrator (RF-19, RF-30) and reminders (RF-26).
CREATE TABLE IF NOT EXISTS notification (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    type            TEXT    NOT NULL CHECK (type IN ('LOW_STOCK', 'LOW_ROTATION', 'APPOINTMENT_REMINDER')),
    audience        TEXT    NOT NULL CHECK (audience IN ('ADMIN', 'CUSTOMER')),
    user_id         INTEGER REFERENCES app_user (id) ON DELETE CASCADE,
    product_id      INTEGER REFERENCES product (id) ON DELETE CASCADE,
    appointment_id  INTEGER REFERENCES appointment (id) ON DELETE CASCADE,
    message         TEXT    NOT NULL,
    created_at      TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    read_at         TEXT
);

-- [D1] Configurable business rules and settings (RF-26): deposit percentage,
-- minimum booking notice, reminder lead time, no-show penalty, rotation window.
CREATE TABLE IF NOT EXISTS app_setting (
    setting_key    TEXT PRIMARY KEY,
    setting_value  TEXT NOT NULL,
    description    TEXT NOT NULL
);

-- =====================================================================
-- Artificial intelligence
-- =====================================================================

-- [AI] Virtual try-on requests and their results (RF-21).
CREATE TABLE IF NOT EXISTS try_on_session (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id        INTEGER NOT NULL REFERENCES customer (id),
    source_image_path  TEXT    NOT NULL,
    transformation     TEXT    NOT NULL,
    result_image_path  TEXT,
    status             TEXT    NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    provider           TEXT    NOT NULL,
    error_message      TEXT,
    created_at         TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- [AI] Conversations of the intelligent assistant (RF-22, RF-23).
CREATE TABLE IF NOT EXISTS assistant_conversation (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL REFERENCES app_user (id),
    started_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- [AI] Messages exchanged in a conversation.
CREATE TABLE IF NOT EXISTS assistant_message (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id  INTEGER NOT NULL REFERENCES assistant_conversation (id) ON DELETE CASCADE,
    sender           TEXT    NOT NULL CHECK (sender IN ('USER', 'ASSISTANT')),
    content          TEXT    NOT NULL,
    created_at       TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- [AI] Audit of the whitelisted tools the assistant asked to run. Tools that modify data
-- wait for the user's confirmation before they are executed (RNF-14).
CREATE TABLE IF NOT EXISTS assistant_tool_call (
    id                     INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id             INTEGER NOT NULL REFERENCES assistant_message (id) ON DELETE CASCADE,
    tool_name              TEXT    NOT NULL,
    arguments              TEXT    NOT NULL DEFAULT '{}',
    result                 TEXT,
    requires_confirmation  INTEGER NOT NULL DEFAULT 0 CHECK (requires_confirmation IN (0, 1)),
    status                 TEXT    NOT NULL DEFAULT 'EXECUTED'
                           CHECK (status IN ('PENDING_CONFIRMATION', 'EXECUTED', 'REJECTED', 'FAILED')),
    executed_at            TEXT
);

-- =====================================================================
-- Indexes
-- =====================================================================

CREATE INDEX IF NOT EXISTS idx_appointment_professional_start ON appointment (professional_id, start_at);
CREATE INDEX IF NOT EXISTS idx_appointment_customer ON appointment (customer_id);
CREATE INDEX IF NOT EXISTS idx_appointment_start ON appointment (start_at);
CREATE INDEX IF NOT EXISTS idx_sale_customer ON sale (customer_id);
CREATE INDEX IF NOT EXISTS idx_sale_sold_at ON sale (sold_at);
CREATE INDEX IF NOT EXISTS idx_sale_line_sale ON sale_line (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_line_product ON sale_line (product_id);
CREATE INDEX IF NOT EXISTS idx_sale_line_service ON sale_line (service_id);
CREATE INDEX IF NOT EXISTS idx_stock_movement_product ON stock_movement (product_id);
CREATE INDEX IF NOT EXISTS idx_notification_unread ON notification (audience, read_at);
CREATE INDEX IF NOT EXISTS idx_assistant_message_conversation ON assistant_message (conversation_id);

-- =====================================================================
-- Triggers
-- =====================================================================

-- Rejects an appointment that overlaps another active one of the same professional (RF-11, RF-12).
CREATE TRIGGER IF NOT EXISTS trg_appointment_no_overlap_insert
BEFORE INSERT ON appointment
WHEN NEW.status IN ('PENDING', 'CONFIRMED')
BEGIN
    SELECT RAISE(ABORT, 'professional_schedule_conflict')
    WHERE EXISTS (
        SELECT 1 FROM appointment a
        WHERE a.professional_id = NEW.professional_id
          AND a.status IN ('PENDING', 'CONFIRMED')
          AND a.start_at < NEW.end_at
          AND a.end_at > NEW.start_at
    );
END;

CREATE TRIGGER IF NOT EXISTS trg_appointment_no_overlap_update
BEFORE UPDATE OF professional_id, start_at, end_at, status ON appointment
WHEN NEW.status IN ('PENDING', 'CONFIRMED')
BEGIN
    SELECT RAISE(ABORT, 'professional_schedule_conflict')
    WHERE EXISTS (
        SELECT 1 FROM appointment a
        WHERE a.id <> NEW.id
          AND a.professional_id = NEW.professional_id
          AND a.status IN ('PENDING', 'CONFIRMED')
          AND a.start_at < NEW.end_at
          AND a.end_at > NEW.start_at
    );
END;

-- Applies every stock movement to the product stock; the CHECK on product.stock
-- aborts the movement (and its transaction) when the stock would become negative.
CREATE TRIGGER IF NOT EXISTS trg_stock_movement_apply
AFTER INSERT ON stock_movement
BEGIN
    UPDATE product SET stock = stock + NEW.quantity WHERE id = NEW.product_id;
END;

-- =====================================================================
-- Views for statistics and alerts
-- =====================================================================

-- Active products at or below their minimum stock (RF-19).
CREATE VIEW IF NOT EXISTS v_low_stock AS
SELECT p.id AS product_id, p.name, p.stock, p.minimum_stock
FROM product p
WHERE p.active = 1 AND p.stock <= p.minimum_stock;

-- Units sold and revenue per product, including products never sold (RF-28, RF-29).
CREATE VIEW IF NOT EXISTS v_product_sales AS
SELECT p.id AS product_id,
       p.name,
       COALESCE(SUM(sl.quantity), 0) AS units_sold,
       COALESCE(SUM(sl.quantity * sl.unit_price), 0) AS revenue,
       MAX(s.sold_at) AS last_sold_at
FROM product p
LEFT JOIN sale_line sl ON sl.product_id = p.id
LEFT JOIN sale s ON s.id = sl.sale_id
GROUP BY p.id, p.name;

-- How many times each service was requested, ignoring cancelled appointments (RF-28).
CREATE VIEW IF NOT EXISTS v_service_demand AS
SELECT bs.id AS service_id,
       bs.name,
       COUNT(a.id) AS appointments_count
FROM beauty_service bs
LEFT JOIN appointment a ON a.service_id = bs.id AND a.status <> 'CANCELLED'
GROUP BY bs.id, bs.name;

-- Appointments and purchases of each customer (RF-18).
CREATE VIEW IF NOT EXISTS v_customer_stats AS
SELECT c.id AS customer_id,
       c.name,
       c.no_show_count,
       (SELECT COUNT(*) FROM appointment a
         WHERE a.customer_id = c.id AND a.status <> 'CANCELLED') AS appointments_count,
       (SELECT COUNT(*) FROM sale s WHERE s.customer_id = c.id) AS purchases_count,
       (SELECT COALESCE(SUM(s.total), 0) FROM sale s WHERE s.customer_id = c.id) AS total_spent
FROM customer c;
