package com.maisonglow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.CustomerService;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/** OpenAI image-edit adapter with local validation and a permanent simulation label. */
public final class ApiTryOnService implements TryOnService {
    private static final String ERROR = "El probador en línea no está disponible. Intenta de nuevo más tarde.";
    private static final int MAX_RESPONSE_CHARS = 20 * 1024 * 1024;
    private final HttpClient client;
    private final URI endpoint;
    private final String model;
    private final String apiKey;
    private final TryOnImages images;
    private final ObjectMapper json = new ObjectMapper();

    /** Creates an adapter with injectable HTTP transport; credentials only go to OpenAI HTTPS. */
    public ApiTryOnService(CustomerService customers, HttpClient client, URI endpoint,
                           String model, String apiKey) {
        this.images = new TryOnImages(customers);
        this.client = Objects.requireNonNull(client, "client");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        if (!"https".equalsIgnoreCase(endpoint.getScheme())
            || !"api.openai.com".equalsIgnoreCase(endpoint.getHost())
            || endpoint.getUserInfo() != null || endpoint.getPort() != -1
            || !"/v1/images/edits".equals(endpoint.getPath())
            || endpoint.getQuery() != null || endpoint.getFragment() != null
            || client.followRedirects() != HttpClient.Redirect.NEVER) {
            throw new IllegalArgumentException("Usa el endpoint HTTPS de edición de OpenAI sin redirecciones.");
        }
        this.model = Objects.requireNonNull(model, "model");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        if (model.isBlank() || apiKey.isBlank()) throw new IllegalArgumentException("Modelo y clave son obligatorios.");
    }

    @Override
    public TryOnResult generate(TryOnRequest request) throws ValidationException, DataAccessException {
        var source = images.validate(request);
        String transformation = request.transformation().trim().toLowerCase(Locale.ROOT);
        try {
            // Re-encode the validated pixels, without the original filename or photo metadata.
            ByteArrayOutputStream photo = new ByteArrayOutputStream();
            if (!ImageIO.write(source, "png", photo)) throw new IOException("PNG encoder unavailable");
            String boundary = "maison-glow-" + UUID.randomUUID();
            String prompt = "Edit only the hair color of the person to " + transformation
                + ". Preserve their identity, face, hairstyle, skin, clothing and background."
                + " This is a cosmetic simulation, not a guarantee of a treatment result.";
            String fields = field(boundary, "model", model) + field(boundary, "prompt", prompt)
                + field(boundary, "n", "1") + field(boundary, "size", "1024x1024")
                + field(boundary, "output_format", "png")
                + "--" + boundary + "\r\nContent-Disposition: form-data; name=\"image\"; filename=\"photo.png\""
                + "\r\nContent-Type: image/png\r\n\r\n";
            var body = HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(fields, StandardCharsets.UTF_8),
                HttpRequest.BodyPublishers.ofByteArray(photo.toByteArray()),
                HttpRequest.BodyPublishers.ofString("\r\n--" + boundary + "--\r\n"));
            HttpRequest http = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary).POST(body).build();
            HttpResponse<String> response = client.send(http, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200 || response.body() == null
                || response.body().length() > MAX_RESPONSE_CHARS) throw new IOException("Invalid image response");
            var root = json.readTree(response.body());
            String encoded = root == null ? "" : root.path("data").path(0).path("b64_json").asText("");
            if (encoded.isBlank()) throw new IOException("Missing image");
            var generated = TryOnImages.read(Base64.getDecoder().decode(encoded));
            return TryOnImages.save(generated, transformation, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DataAccessException(ERROR);
        } catch (IOException | IllegalArgumentException | ValidationException e) {
            // Do not expose provider bodies, credentials or photo data to the UI/logs.
            throw new DataAccessException(ERROR);
        }
    }

    private static String field(String boundary, String name, String value) {
        return "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
            + "\"\r\n\r\n" + value + "\r\n";
    }
}
