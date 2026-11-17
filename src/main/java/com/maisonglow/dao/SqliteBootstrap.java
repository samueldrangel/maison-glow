package com.maisonglow.dao;

import com.maisonglow.exception.DataAccessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Imports legacy files once in one transaction. Originals remain available for rollback. */
public final class SqliteBootstrap {
    private SqliteBootstrap() { }
    private static final Map<String,String> TABLES = new LinkedHashMap<>();
    static {
        TABLES.put("categories.txt", "product_category|id,name");
        TABLES.put("customers.txt", "customer|id,document,name,phone,email,no_show_count,registered_at");
        TABLES.put("treatments.txt", "treatment|id,name,description,price,duration_minutes,active");
        TABLES.put("professionals.txt", "professional|id,document,name,phone,email,specialty,work_start,work_end,active");
        TABLES.put("professional_treatments.txt", "professional_treatment|professional_id,treatment_id");
        TABLES.put("products.txt", "product|id,name,category_id,price,stock,minimum_stock,active");
        TABLES.put("appointments.txt", "appointment|id,customer_id,professional_id,treatment_id,start_at,end_at,status,deposit,reminder_at,created_at");
        TABLES.put("sales.txt", "sale|id,customer_id,appointment_id,sold_at,total");
        TABLES.put("sale_lines.txt", "sale_line|id,sale_id,product_id,treatment_id,promotion_id,quantity,unit_price");
        TABLES.put("settings.txt", "app_setting|setting_key,setting_value,description");
    }

    public static SqliteDatabase open(Path folder) throws DataAccessException {
        SqliteDatabase database = new SqliteDatabase(folder.resolve("maison-glow.db"));
        database.initialize();
        database.transaction(c -> {
            try(var statement=c.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS app_metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
                try(var rows=statement.executeQuery("SELECT 1 FROM app_metadata WHERE key='legacy_import_v1'")) {
                    if(rows.next()) return null;
                }
            }
            for(var entry:TABLES.entrySet()) importFile(c,folder.resolve(entry.getKey()),entry.getValue());
            database.runScript("seed.sql");
            try(var statement=c.createStatement()) {
                statement.executeUpdate("INSERT INTO app_metadata VALUES ('legacy_import_v1','complete')");
            }
            return null;
        });
        return database;
    }

    private static void importFile(Connection c,Path path,String descriptor) throws SQLException,DataAccessException {
        if(!Files.exists(path)) return;
        String[] parts=descriptor.split("\\|");
        String table=parts[0]; String[] columns=parts[1].split(",");
        String slots=String.join(",",java.util.Collections.nCopies(columns.length,"?"));
        try(var statement=c.prepareStatement("INSERT INTO "+table+" ("+parts[1]+") VALUES ("+slots+")")) {
            int nextId=1;
            for(String line:Files.readAllLines(path)) {
                if(line.isBlank()) continue;
                if(line.startsWith("#nextId=")) { nextId=Integer.parseInt(line.substring(8)); continue; }
                String[] fields=TextLineCodec.decode(line);
                if(fields.length!=columns.length) throw new DataAccessException("Datos inválidos en "+path+". La importación no se aplicó.");
                for(int i=0;i<fields.length;i++) {
                    boolean nullable=columns[i].equals("reminder_at") || columns[i].equals("appointment_id") || columns[i].equals("product_id")
                        || columns[i].equals("treatment_id") || columns[i].equals("promotion_id");
                    statement.setObject(i+1,nullable && fields[i].isEmpty()?null:fields[i]);
                }
                statement.executeUpdate();
            }
            if(columns[0].equals("id")) {
                try(var update=c.prepareStatement("UPDATE sqlite_sequence SET seq=MAX(seq,?) WHERE name=?")) {
                    update.setInt(1,nextId-1); update.setString(2,table);
                    if(update.executeUpdate()==0 && nextId>1) {
                        try(var insert=c.prepareStatement("INSERT INTO sqlite_sequence(name,seq) VALUES (?,?)")) {
                            insert.setString(1,table); insert.setInt(2,nextId-1); insert.executeUpdate();
                        }
                    }
                }
            }
        } catch(IOException | IllegalArgumentException e) { throw new DataAccessException("No se pudo importar "+path+". Los archivos originales se conservan.",e); }
    }
}
