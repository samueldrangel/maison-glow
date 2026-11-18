package com.maisonglow.service;

import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import javax.imageio.ImageIO;

/** Validates imports and owns image filenames. Camera captures use the same import path as uploaded files. */
public final class PhotoStore {
    private final Path root;
    public PhotoStore(Path root) { this.root=root.toAbsolutePath().normalize(); }
    public String importPhoto(Path source) throws ValidationException,DataAccessException {
        if(source==null) throw new ValidationException("Selecciona una foto.");
        try {
            if(!Files.isRegularFile(source)||Files.size(source)<=0||Files.size(source)>10*1024*1024)
                throw new ValidationException("La foto debe ser JPG o PNG de hasta 10 MB.");
            BufferedImage image;
            try(var input=ImageIO.createImageInputStream(source.toFile())) {
                if(input==null) throw new ValidationException("No se pudo leer la foto.");
                var readers=ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new ValidationException("La foto debe ser JPG o PNG.");
                var reader=readers.next();
                try {
                    String format=reader.getFormatName();reader.setInput(input);
                    if(!(format.equalsIgnoreCase("JPEG")||format.equalsIgnoreCase("PNG"))||reader.getWidth(0)>4096||reader.getHeight(0)>4096)
                        throw new ValidationException("La foto debe ser JPG o PNG de hasta 4096 píxeles por lado.");
                    image=reader.read(0);
                } finally {reader.dispose();}
            }
            Files.createDirectories(root);
            String key=UUID.randomUUID()+".png";
            Path destination=resolve(key);
            try {
                if(!ImageIO.write(image,"png",destination.toFile())) throw new IOException("PNG encoder unavailable");
                return key;
            } catch(IOException|RuntimeException e) {Files.deleteIfExists(destination);throw e;}
        } catch(IOException|SecurityException e) { throw new DataAccessException("No se pudo conservar la foto.",e); }
    }
    public Path resolve(String key) throws DataAccessException {
        if(key==null||!key.matches("[a-f0-9-]{36}\\.png")) throw new DataAccessException("Referencia de foto inválida.");
        Path path=root.resolve(key).normalize();
        if(!path.getParent().equals(root)||Files.isSymbolicLink(root)||Files.isSymbolicLink(path)) throw new DataAccessException("Referencia de foto inválida.");
        return path;
    }
    public void delete(String key) throws DataAccessException {
        if(key==null)return;
        try {Files.deleteIfExists(resolve(key));}
        catch(IOException e){throw new DataAccessException("No se pudo retirar la foto.",e);}
    }
    /** Failed filesystem deletions stay queued for the next startup; never undo an already committed edit. */
    public void cleanPending(com.maisonglow.dao.CommerceDao store) {
        try {for(String key:store.pendingPhotoDeletes())try{delete(key);store.photoDeleted(key);}catch(DataAccessException ignored){}}
        catch(DataAccessException ignored) { /* Retry at the next explicit cleanup/startup. */ }
    }
}
