-- Additive migration 2: publication, orders/reservations and private change history.
CREATE TABLE IF NOT EXISTS product_listing (
    product_id INTEGER PRIMARY KEY REFERENCES product(id),
    published INTEGER NOT NULL DEFAULT 0 CHECK(published IN (0,1)),
    cover_key TEXT
);
CREATE TABLE IF NOT EXISTS customer_order (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL REFERENCES customer(id),
    request_key TEXT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('PENDING_PAYMENT','PAID','CANCELLED','EXPIRED')),
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL,
    sale_id INTEGER UNIQUE REFERENCES sale(id),
    total TEXT NOT NULL CHECK(CAST(total AS NUMERIC)>0),
    UNIQUE(customer_id,request_key)
);
CREATE TABLE IF NOT EXISTS order_item (
    order_id INTEGER NOT NULL REFERENCES customer_order(id),
    product_id INTEGER NOT NULL REFERENCES product(id),
    name TEXT NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity>0),
    unit_price TEXT NOT NULL CHECK(CAST(unit_price AS NUMERIC)>0),
    PRIMARY KEY(order_id,product_id)
);
CREATE INDEX IF NOT EXISTS idx_order_customer ON customer_order(customer_id,id);
CREATE INDEX IF NOT EXISTS idx_order_expiry ON customer_order(status,expires_at);
CREATE INDEX IF NOT EXISTS idx_order_item_product ON order_item(product_id);
CREATE TABLE IF NOT EXISTS customer_photo_access (
    customer_id INTEGER PRIMARY KEY REFERENCES customer(id),
    allowed INTEGER NOT NULL CHECK(allowed IN (0,1))
);
CREATE TABLE IF NOT EXISTS customer_change (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL REFERENCES customer(id),
    author_id INTEGER NOT NULL REFERENCES app_user(id),
    appointment_id INTEGER REFERENCES appointment(id),
    kind TEXT NOT NULL CHECK(kind IN ('PERSONAL','SERVICE','SIMULATION')),
    notes TEXT NOT NULL,
    happened_on TEXT NOT NULL,
    created_at TEXT NOT NULL,
    source_key TEXT,
    result_key TEXT
);
CREATE INDEX IF NOT EXISTS idx_change_customer ON customer_change(customer_id,happened_on);
CREATE TABLE IF NOT EXISTS media_delete_queue (photo_key TEXT PRIMARY KEY);
-- All writers, including the existing cash-register service, must respect reserved units.
CREATE TRIGGER IF NOT EXISTS trg_product_reserved_stock
BEFORE UPDATE OF stock ON product
WHEN NEW.stock < COALESCE((SELECT SUM(i.quantity) FROM order_item i JOIN customer_order o ON o.id=i.order_id WHERE i.product_id=NEW.id AND o.status='PENDING_PAYMENT'),0)
BEGIN
    SELECT RAISE(ABORT,'reserved_stock_conflict');
END;
