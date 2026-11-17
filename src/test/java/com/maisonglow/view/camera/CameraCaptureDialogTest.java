package com.maisonglow.view.camera;

import com.maisonglow.view.FxTest;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CameraCaptureDialogTest {
    @TempDir Path folder;
    private CameraCaptureDialog capture;
    @BeforeAll static void startFx() { assumeTrue(FxTest.start()); }
    @AfterEach void close() throws Exception { if(capture!=null) FxTest.onFxThread(() -> {capture.stop();return null;}); }
    private static class FakeCamera implements PhotoCamera {
        final AtomicInteger closes=new AtomicInteger();
        final AtomicInteger opens=new AtomicInteger();
        final BufferedImage image=new BufferedImage(320,240,BufferedImage.TYPE_INT_RGB);
        public void open() throws IOException { opens.incrementAndGet(); }
        public BufferedImage read() { return image; }
        public void close() { closes.incrementAndGet(); }
    }
    private void open(PhotoCamera camera) throws Exception {
        capture=FxTest.onFxThread(() -> {
            var view=new CameraCaptureDialog(camera,folder);
            var pane=view.dialog().getDialogPane();pane.resize(700,650);pane.applyCss();pane.layout();
            view.start();return view;
        });
    }
    private Button button(String id) { return (Button)capture.dialog().getDialogPane().lookup("#"+id); }
    private void press(String id) throws Exception { FxTest.onFxThread(() -> {button(id).fire();return null;}); }
    private void await(BooleanSupplier condition) throws Exception {
        for(int i=0;i<120;i++) { if(FxTest.onFxThread(condition::getAsBoolean)) return;Thread.sleep(25); }
        assertThat(FxTest.onFxThread(condition::getAsBoolean)).isTrue();
    }
    @Test void previewsCapturesRetakesAndAcceptsOnlyTheConfirmedImage() throws Exception {
        FakeCamera camera=new FakeCamera();open(camera);
        await(() -> !button("captureButton").isDisabled());
        FxTest.onFxThread(() -> {
            var pane=capture.dialog().getDialogPane();pane.applyCss();pane.layout();
            var snapshot=pane.snapshot(null,null);
            BufferedImage rendered=new BufferedImage((int)snapshot.getWidth(),(int)snapshot.getHeight(),BufferedImage.TYPE_INT_ARGB);
            var pixels=snapshot.getPixelReader();
            for(int y=0;y<rendered.getHeight();y++) for(int x=0;x<rendered.getWidth();x++) rendered.setRGB(x,y,pixels.getArgb(x,y));
            try {Path output=Path.of("target","visual-qa","camera-dialog.png");Files.createDirectories(output.getParent());ImageIO.write(rendered,"png",output.toFile());}
            catch(IOException e){throw new java.io.UncheckedIOException(e);}return null;
        });
        press("captureButton");
        assertThat(FxTest.onFxThread(() -> button("usePhotoButton").isDisabled())).isFalse();
        press("retakeButton");
        assertThat(FxTest.onFxThread(() -> button("usePhotoButton").isDisabled())).isTrue();
        press("captureButton");press("usePhotoButton");
        await(() -> capture.dialog().getResult()!=null);
        Path image=FxTest.onFxThread(() -> capture.dialog().getResult());
        assertThat(image).exists();assertThat(ImageIO.read(image.toFile()).getWidth()).isEqualTo(320);
        FxTest.onFxThread(() -> {capture.stop();return null;});
        for(int i=0;i<80 && camera.closes.get()==0;i++) Thread.sleep(25);
        assertThat(camera.closes.get()).isPositive();
    }
    @Test void noCameraShowsRecoveryAndDoesNotSaveAnything() throws Exception {
        FakeCamera camera=new FakeCamera() { public List<String> devices() {return List.of();} };
        open(camera);
        await(() -> ((Label)capture.dialog().getDialogPane().lookup("#cameraStatus")).getText().contains("No se encontró"));
        assertThat(FxTest.onFxThread(() -> button("captureButton").isDisabled())).isTrue();
        try(var files=Files.list(folder)) {assertThat(files.count()).isZero();}
    }
    @Test void cancellationDuringOpeningClosesTheLateCameraWithoutSaving() throws Exception {
        CountDownLatch opening=new CountDownLatch(1),release=new CountDownLatch(1);
        FakeCamera camera=new FakeCamera() {
            @Override public void open() throws IOException {
                opening.countDown();try {release.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException(e);}
            }
        };
        open(camera);assertThat(opening.await(2,TimeUnit.SECONDS)).isTrue();
        FxTest.onFxThread(() -> {capture.stop();return null;});release.countDown();
        for(int i=0;i<80 && camera.closes.get()==0;i++) Thread.sleep(25);
        assertThat(camera.closes.get()).isPositive();
        try(var files=Files.list(folder)) {assertThat(files.count()).isZero();}
    }
    @Test void permissionFailureAllowsSelectingAnotherCamera() throws Exception {
        FakeCamera camera=new FakeCamera() {
            String source;
            @Override public List<String> devices() {return List.of("Bloqueada","Disponible");}
            @Override public void select(String name) {source=name;}
            @Override public void open() throws IOException {if(source.equals("Bloqueada")) throw new IOException("Cámara sin permisos");super.open();}
        };
        open(camera);
        await(() -> ((Label)capture.dialog().getDialogPane().lookup("#cameraStatus")).getText().contains("sin permisos"));
        FxTest.onFxThread(() -> {((ComboBox<String>)capture.dialog().getDialogPane().lookup("#cameraSource")).setValue("Disponible");return null;});
        await(() -> !button("captureButton").isDisabled());
        assertThat(camera.opens.get()).isEqualTo(1);
    }
}
