package com.maisonglow.view.camera;

import com.github.sarxos.webcam.Webcam;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Local integrated/USB webcam adapter. No external executable or upload is involved. */
public final class WebcamPhotoCamera implements PhotoCamera {
    private Webcam webcam;
    private java.util.List<Webcam> devices;
    private String selected;

    @Override public java.util.List<String> devices() throws IOException {
        try {
            devices = Webcam.getWebcams(8, TimeUnit.SECONDS).stream()
                .sorted(Comparator.comparingInt(w -> w.getName().toLowerCase(java.util.Locale.ROOT).matches(".*(virtual|share|obs|droidcam).*") ? 1 : 0)).toList();
            return devices.stream().map(Webcam::getName).toList();
        } catch (TimeoutException | RuntimeException | LinkageError e) {
            throw new IOException("No se pudieron consultar las cámaras. Revisa los permisos o usa Subir foto.", e);
        }
    }
    @Override public void select(String name) { selected=name; }

    @Override public void open() throws IOException {
        try {
            if (devices == null) devices();
            webcam = devices.stream().filter(w -> selected == null || w.getName().equals(selected)).findFirst().orElse(null);
            if (webcam == null) throw new IOException("No se encontró una cámara. Conecta una cámara o usa Subir foto.");
            Dimension size = Arrays.stream(webcam.getViewSizes())
                .filter(d -> d.width <= 1280 && d.height <= 960)
                .max(Comparator.comparingInt(d -> d.width * d.height)).orElse(webcam.getViewSize());
            webcam.setViewSize(size);
            if (!webcam.open()) throw new IOException("No se pudo abrir la cámara. Comprueba sus permisos y que no esté en uso.");
        } catch (IOException e) {
            close(); throw e;
        } catch (RuntimeException | LinkageError e) {
            close();
            throw new IOException("No se pudo abrir la cámara. Comprueba sus permisos y cierra otras aplicaciones que la usen.", e);
        }
    }
    @Override public BufferedImage read() throws IOException {
        try { return webcam == null ? null : webcam.getImage(); }
        catch (RuntimeException e) { throw new IOException("Se perdió la conexión con la cámara. Vuelve a abrirla o usa Subir foto.", e); }
    }
    @Override public void close() {
        if (webcam != null) {
            try { webcam.close(); } catch (RuntimeException | LinkageError ignored) { /* Cleanup must not block window closure. */ }
            webcam = null;
        }
    }
}
