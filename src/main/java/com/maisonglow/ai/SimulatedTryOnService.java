package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.CustomerService;
import com.maisonglow.util.Validator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Offline visual preview with a color tint; it does not predict a real treatment result. */
public final class SimulatedTryOnService implements TryOnService {
    /** Warning shared by the image and result. */
    public static final String DISCLAIMER = "simulación, no garantía";
    private static final Map<String, Color> COLORS = Map.of(
        "rojo cobrizo", new Color(172, 69, 39),
        "rubio dorado", new Color(207, 160, 62),
        "castaño oscuro", new Color(80, 48, 35));
    private static final long MAX_PHOTO_BYTES = 10L * 1024 * 1024;
    private static final int MAX_DIMENSION = 4096;
    private static final int LABEL_HEIGHT = 56;

    private final CustomerService customers;

    /** Creates an offline try-on service that checks customers through the business service. */
    public SimulatedTryOnService(CustomerService customers) {
        this.customers = Objects.requireNonNull(customers, "customers");
    }

    /** Returns the transformation names to offer in the future JavaFX selector. */
    public static List<String> supportedTransformations() {
        return List.of("rojo cobrizo", "rubio dorado", "castaño oscuro");
    }

    @Override
    public TryOnResult generate(TryOnRequest request) throws ValidationException, DataAccessException {
        Validator.requirePresent(request, "solicitud");
        if (request.customerId() <= 0) throw new ValidationException("Selecciona un cliente registrado.");
        Validator.requireText(request.transformation(), "transformación");
        String transformation = request.transformation().trim().toLowerCase(Locale.ROOT);
        Color tint = COLORS.get(transformation);
        if (tint == null) throw new ValidationException("Transformación no disponible. Elige una opción de la lista.");
        Validator.requirePresent(request.photo(), "foto");
        Path photo = request.photo();
        try {
            if (!Files.isRegularFile(photo) || Files.size(photo) == 0 || Files.size(photo) > MAX_PHOTO_BYTES) {
                throw new ValidationException("Selecciona una foto PNG o JPEG de hasta 10 MB.");
            }
        } catch (IOException | SecurityException e) {
            throw new ValidationException("No se pudo leer la foto seleccionada.", e);
        }
        BufferedImage source;
        try (ImageInputStream input = ImageIO.createImageInputStream(photo.toFile())) {
            if (input == null) throw new ValidationException("La foto no es una imagen PNG o JPEG válida.");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ValidationException("La foto no es una imagen PNG o JPEG válida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) {
                    throw new ValidationException("La foto no es una imagen PNG o JPEG válida.");
                }
                if (reader.getWidth(0) > MAX_DIMENSION || reader.getHeight(0) > MAX_DIMENSION) {
                    throw new ValidationException("La foto supera el tamaño máximo de 4096 píxeles por lado.");
                }
                source = reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | SecurityException e) {
            throw new ValidationException("La foto no es una imagen PNG o JPEG válida.", e);
        }
        if (source == null || source.getWidth() < 1 || source.getHeight() < 1) {
            throw new ValidationException("La foto no es una imagen PNG o JPEG válida.");
        }
        try {
            customers.findById(request.customerId());
        } catch (BusinessRuleException e) {
            throw new ValidationException("Selecciona un cliente registrado.", e);
        }

        int width = Math.max(source.getWidth(), 240);
        BufferedImage preview = new BufferedImage(width, source.getHeight() + LABEL_HEIGHT,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = preview.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, source.getHeight());
            graphics.drawImage(source, 0, 0, null);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            graphics.setColor(tint);
            graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.setColor(new Color(50, 33, 44));
            graphics.fillRect(0, source.getHeight(), width, LABEL_HEIGHT);
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            graphics.drawString(DISCLAIMER, 8, source.getHeight() + 23);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            graphics.drawString(transformation, 8, source.getHeight() + 43);
        } finally {
            graphics.dispose();
        }
        try {
            Path image = Files.createTempFile("maison-glow-try-on-", ".png");
            image.toFile().deleteOnExit();
            if (!ImageIO.write(preview, "png", image.toFile())) {
                throw new IOException("No hay codificador PNG disponible.");
            }
            return new TryOnResult(image, DISCLAIMER);
        } catch (IOException | SecurityException e) {
            throw new DataAccessException("No se pudo guardar la simulación.", e);
        }
    }
}
