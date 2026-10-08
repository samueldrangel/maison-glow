-- Maison Glow - default settings and categories.
-- Safe to run more than once.

INSERT OR IGNORE INTO app_setting (setting_key, setting_value, description) VALUES
    ('business_name', 'Maison Glow', 'Name shown on invoices and screens'),
    ('deposit_percentage', '30', 'Percentage of the service price required as deposit when booking'),
    ('min_booking_notice_hours', '2', 'Minimum hours between booking and the appointment start'),
    ('reminder_hours_before', '24', 'Hours before an appointment when the reminder is generated'),
    ('no_show_penalty_threshold', '3', 'No-shows after which a customer must pay the full service price in advance'),
    ('low_rotation_window_days', '30', 'Days of sales history used to detect low-rotation products'),
    ('low_rotation_top_n', '5', 'How many low-rotation products are reported to the administrator'),
    ('low_rotation_discount_percentage', '15', 'Default discount offered for low-rotation products');

INSERT OR IGNORE INTO product_category (name) VALUES
    ('Cabello'),
    ('Piel'),
    ('Uñas'),
    ('Maquillaje'),
    ('Accesorios');
