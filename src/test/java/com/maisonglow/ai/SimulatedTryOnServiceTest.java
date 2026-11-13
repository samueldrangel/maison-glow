package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.model.Customer;
import com.maisonglow.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class SimulatedTryOnServiceTest {
    @TempDir Path directory;
    private final CustomerService customers = mock(CustomerService.class);
    private final SimulatedTryOnService service = new SimulatedTryOnService(customers);

    private Path photo(String format) throws Exception {
        Path file = directory.resolve("foto." + format);
        BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();
        ImageIO.write(image, format, file.toFile());
        return file;
    }

    @Test
    void validRequestProducesLabeledPngWithoutChangingSource() throws Exception {
        Path original = photo("png");
        byte[] originalBytes = Files.readAllBytes(original);
        when(customers.findById(1)).thenReturn(new Customer(1, "123", "Ana", "3001234567", "ana@example.com"));

        TryOnResult result = service.generate(new TryOnRequest(1, original, " Rojo Cobrizo "));

        assertThat(result.disclaimer()).isEqualTo("simulación, no garantía");
        assertThat(result.image()).exists().hasExtension("png");
        assertThat(result.image()).isNotEqualTo(original);
        assertThat(Files.readAllBytes(original)).isEqualTo(originalBytes);
        BufferedImage output = ImageIO.read(result.image().toFile());
        assertThat(output.getWidth()).isEqualTo(400);
        assertThat(output.getHeight()).isEqualTo(356);
        assertThat(output.getRGB(200, 100)).isNotEqualTo(Color.WHITE.getRGB());
        assertThat(output.getRGB(200, 320)).isNotEqualTo(Color.WHITE.getRGB());
        verify(customers).findById(1);
    }

    @Test
    void jpegAndEverySupportedTransformationWork() throws Exception {
        Path original = photo("jpg");
        when(customers.findById(1)).thenReturn(new Customer(1, "123", "Ana", "3001234567", "ana@example.com"));
        for (String transformation : SimulatedTryOnService.supportedTransformations()) {
            TryOnResult result = service.generate(new TryOnRequest(1, original, transformation));
            assertThat(ImageIO.read(result.image().toFile())).isNotNull();
            assertThat(result.disclaimer()).contains("simulación");
        }
    }

    @Test
    void missingOrUnregisteredCustomerIsValidationError() throws Exception {
        Path original = photo("png");
        assertThatThrownBy(() -> service.generate(new TryOnRequest(0, original, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("cliente registrado");
        when(customers.findById(9)).thenThrow(new BusinessRuleException("No existe el cliente"));
        assertThatThrownBy(() -> service.generate(new TryOnRequest(9, original, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("cliente registrado");
    }

    @Test
    void invalidRequestsFailBeforeLookingUpCustomer() throws Exception {
        Path original = photo("png");
        assertThatThrownBy(() -> service.generate(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, original, "  ")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, original, "azul neón")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("Transformación no disponible");
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, null, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, directory.resolve("missing.png"), "rojo cobrizo")))
            .isInstanceOf(ValidationException.class);
        Path corrupt = directory.resolve("corrupt.png");
        Files.writeString(corrupt, "not an image");
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, corrupt, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("imagen PNG o JPEG");
        Path gif = photo("gif");
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, gif, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class).hasMessageContaining("imagen PNG o JPEG");
        verifyNoInteractions(customers);
    }

    @Test
    void storageFailurePropagatesWithoutCreatingImage() throws Exception {
        Path original = photo("png");
        when(customers.findById(1)).thenThrow(new DataAccessException("falló almacenamiento"));
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, original, "rojo cobrizo")))
            .isInstanceOf(DataAccessException.class).hasMessageContaining("falló almacenamiento");
    }
}
