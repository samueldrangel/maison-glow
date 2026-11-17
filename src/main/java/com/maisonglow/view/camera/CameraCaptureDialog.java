package com.maisonglow.view.camera;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Live preview, freeze/retake and explicit acceptance; the camera closes on every exit. */
public final class CameraCaptureDialog {
    private final PhotoCamera camera;
    private final Path folder;
    private final Dialog<Path> dialog = new Dialog<>();
    private final ImageView preview = new ImageView();
    private final Label status = new Label("Abriendo cámara…");
    private final Button capture = new Button("Capturar");
    private final Button retake = new Button("Volver a tomar");
    private final Button accept;
    private final ComboBox<String> sources = new ComboBox<>();
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task,"camara-probador"); thread.setDaemon(true); return thread;
    });
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicBoolean pendingFrame = new AtomicBoolean();
    private volatile boolean frozen;
    private volatile BufferedImage lastFrame;
    private BufferedImage selectedFrame;
    private boolean started;
    private int emptyFrames;
    private volatile boolean ready;
    private volatile String activeSource;

    public CameraCaptureDialog(PhotoCamera camera, Path folder) {
        this.camera = java.util.Objects.requireNonNull(camera);
        this.folder = java.util.Objects.requireNonNull(folder);
        dialog.setTitle("Tomar foto del cliente");
        dialog.setHeaderText("Encuadra al cliente y captura la foto. Puedes repetirla antes de usarla.");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/maisonglow/view/styles.css").toExternalForm());
        dialog.getDialogPane().getStyleClass().addAll("view-root","camera-dialog");
        ButtonType use = new ButtonType("Usar esta foto",ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(use,ButtonType.CANCEL);
        accept = (Button)dialog.getDialogPane().lookupButton(use);
        accept.setId("usePhotoButton"); accept.setDisable(true);
        accept.getStyleClass().add("button-gold");
        accept.addEventFilter(ActionEvent.ACTION, event -> { event.consume(); saveSelected(); });
        dialog.setResultConverter(button -> null);
        preview.setId("cameraPreview"); preview.setPreserveRatio(true); preview.setFitWidth(640); preview.setFitHeight(420);
        preview.setAccessibleText("Vista previa de la cámara del cliente");
        StackPane frame = new StackPane(preview); frame.getStyleClass().add("photo-frame");
        frame.setMinHeight(260); frame.setPrefSize(640,420);
        capture.setId("captureButton"); retake.setId("retakeButton"); status.setId("cameraStatus"); status.setWrapText(true);
        capture.getStyleClass().add("button-primary"); retake.getStyleClass().add("button-secondary");
        capture.setDisable(true); retake.setDisable(true);
        capture.setOnAction(event -> {
            if (lastFrame == null) return;
            frozen = true;
            selectedFrame = copy(lastFrame);
            preview.setImage(toImage(selectedFrame));
            capture.setDisable(true); retake.setDisable(false); accept.setDisable(false);
            status.setText("Foto capturada. Puedes volver a tomarla o usar esta foto.");
        });
        retake.setOnAction(event -> {
            selectedFrame=null; frozen=false; accept.setDisable(true); retake.setDisable(true);
            capture.setDisable(lastFrame==null); status.setText("Vista previa en directo. Pulsa Capturar cuando estés listo.");
        });
        sources.setId("cameraSource"); sources.setMaxWidth(Double.MAX_VALUE); sources.setDisable(true);
        sources.setAccessibleText("Seleccionar cámara");
        sources.setOnAction(event -> {
            String selected=sources.getValue();
            if(selected==null || selected.equals(activeSource) || stopped.get()) return;
            ready=false; frozen=false; selectedFrame=null; lastFrame=null;
            preview.setImage(null); capture.setDisable(true); retake.setDisable(true); accept.setDisable(true); sources.setDisable(true);
            status.setText("Abriendo cámara…");
            worker.execute(() -> { camera.close(); openSource(selected); });
        });
        Label sourceLabel=new Label("Cámara"); sourceLabel.setLabelFor(sources);
        VBox content = new VBox(12,sourceLabel,sources,frame,status,new HBox(12,capture,retake));
        dialog.getDialogPane().setContent(content);
        dialog.setOnShown(event -> start());
        dialog.setOnHidden(event -> stop());
        dialog.getDialogPane().setPrefWidth(700);
    }

    public Path show(Window owner) {
        if (owner != null) dialog.initOwner(owner);
        return dialog.showAndWait().orElse(null);
    }

    // Package access allows lifecycle verification without accessing a physical camera.
    Dialog<Path> dialog() { return dialog; }
    void start() {
        if (started || stopped.get()) return;
        started=true;
        worker.execute(() -> {
            try {
                var available=camera.devices();
                if(available.isEmpty()) throw new IOException("No se encontró una cámara. Conecta una cámara o usa Subir foto.");
                activeSource=available.getFirst();
                Platform.runLater(() -> { if(!stopped.get()) { sources.getItems().setAll(available); sources.setValue(activeSource); } });
                openSource(activeSource);
                if (stopped.get()) return;
                worker.scheduleWithFixedDelay(this::readFrame,0,100,TimeUnit.MILLISECONDS);
            } catch (IOException | RuntimeException | LinkageError e) { fail(e); }
        });
    }
    private void openSource(String name) {
        try {
            activeSource=name;
            camera.select(name); camera.open();
            if(stopped.get()) { camera.close(); return; }
            activeSource=name; ready=true; emptyFrames=0;
            Platform.runLater(() -> { if(!stopped.get()) { sources.setDisable(false); status.setText("Vista previa en directo. Pulsa Capturar cuando estés listo."); } });
        } catch(IOException | RuntimeException | LinkageError e) { fail(e); }
    }
    private void readFrame() {
        if (stopped.get() || !ready || frozen || !pendingFrame.compareAndSet(false,true)) return;
        try {
            String source=activeSource;
            BufferedImage frame = camera.read();
            if (frame == null) {
                pendingFrame.set(false);
                if (++emptyFrames >= 80) throw new IOException("La cámara no está enviando imagen. Vuelve a abrirla o usa Subir foto.");
                return;
            }
            emptyFrames=0;
            lastFrame=frame;
            WritableImage image=toImage(frame);
            Platform.runLater(() -> {
                try { if (!stopped.get() && !frozen && ready && java.util.Objects.equals(source,activeSource)) { preview.setImage(image); capture.setDisable(false); } }
                finally { pendingFrame.set(false); }
            });
        } catch (IOException | RuntimeException | LinkageError e) { pendingFrame.set(false); fail(e); }
    }
    private void saveSelected() {
        if (selectedFrame==null || stopped.get()) return;
        BufferedImage photo=selectedFrame;
        accept.setDisable(true); retake.setDisable(true); status.setText("Guardando foto…");
        sources.setDisable(true);
        worker.execute(() -> {
            Path file=null;
            try {
                Files.createDirectories(folder);
                file=Files.createTempFile(folder,"cliente-", ".png");
                if (!ImageIO.write(photo,"png",file.toFile())) throw new IOException("No se pudo codificar la foto.");
                Path saved=file;
                Platform.runLater(() -> {
                    if (stopped.get()) { delete(saved); return; }
                    dialog.setResult(saved); dialog.close();
                });
            } catch (IOException | RuntimeException e) {
                if (file!=null) delete(file);
                Platform.runLater(() -> {
                    if (!stopped.get()) { status.setText("No se pudo guardar la foto. Revisa la carpeta de datos e inténtalo de nuevo."); accept.setDisable(false); retake.setDisable(false); sources.setDisable(false); }
                });
            }
        });
    }
    private void fail(Throwable error) {
        ready=false;
        camera.close();
        Platform.runLater(() -> {
            if(stopped.get()) return;
            status.setText(error instanceof IOException ? error.getMessage() : "No se pudo usar la cámara. Revisa sus permisos o usa Subir foto.");
            capture.setDisable(true); retake.setDisable(true); accept.setDisable(true);
            sources.setDisable(sources.getItems().isEmpty());
        });
    }
    void stop() {
        if (!stopped.compareAndSet(false,true)) return;
        worker.execute(camera::close);
        worker.shutdown();
    }
    private static void delete(Path file) {
        try { Files.deleteIfExists(file); } catch (IOException ignored) { /* A canceled capture is never selected. */ }
    }
    private static BufferedImage copy(BufferedImage source) {
        BufferedImage result=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_RGB);
        var graphics=result.createGraphics();
        try { graphics.drawImage(source,0,0,null); } finally { graphics.dispose(); }
        return result;
    }
    private static WritableImage toImage(BufferedImage source) {
        int w=source.getWidth(),h=source.getHeight();
        WritableImage image=new WritableImage(w,h);
        image.getPixelWriter().setPixels(0,0,w,h,PixelFormat.getIntArgbInstance(),source.getRGB(0,0,w,h,null,0,w),0,w);
        return image;
    }
}
