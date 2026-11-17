package com.maisonglow.view.camera;

import java.awt.image.BufferedImage;
import java.io.IOException;

/** Camera resource owned by one capture window; all calls run off the JavaFX thread. */
public interface PhotoCamera extends AutoCloseable {
    default java.util.List<String> devices() throws IOException { return java.util.List.of("Cámara predeterminada"); }
    default void select(String name) { }
    void open() throws IOException;
    BufferedImage read() throws IOException;
    @Override void close();
}
