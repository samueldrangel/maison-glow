-- Datos ficticios para probar la aplicación. Se aplican una sola vez al preparar la sesión.
INSERT OR IGNORE INTO customer(document,name,phone,email) VALUES
 ('DEMO-1001','Ana García (demo)','3001112233','ana@example.com'),
 ('DEMO-1002','Valentina Torres (demo)','3002223344','valentina@example.com'),
 ('DEMO-1003','Laura Martínez (demo)','3003334455','laura@example.com');
INSERT OR IGNORE INTO treatment(name,description,price,duration_minutes) VALUES
 ('Coloración de cabello','Coloración y asesoría de tono',120000,90),
 ('Corte de cabello','Corte y peinado',55000,45),
 ('Manicure','Cuidado y esmaltado de uñas',35000,45);
INSERT OR IGNORE INTO professional(document,name,phone,email,specialty,work_start,work_end) VALUES
 ('DEMO-2001','Camila López (demo)','3004445566','camila@example.com','Coloración y corte','08:00','18:00');
INSERT OR IGNORE INTO professional_treatment(professional_id,treatment_id)
 SELECT p.id,t.id FROM professional p CROSS JOIN treatment t
 WHERE p.document='DEMO-2001' AND t.name IN ('Coloración de cabello','Corte de cabello');
INSERT OR IGNORE INTO product(name,category_id,price,stock,minimum_stock) VALUES
 ('Champú hidratante', (SELECT id FROM product_category WHERE name='Cabello'),28000,12,5),
 ('Mascarilla reparadora', (SELECT id FROM product_category WHERE name='Cabello'),35000,2,5),
 ('Aceite capilar', (SELECT id FROM product_category WHERE name='Cabello'),42000,8,3);
