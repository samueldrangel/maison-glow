package com.maisonglow.ai;

import com.maisonglow.exception.BusinessRuleException;
import com.maisonglow.exception.DataAccessException;
import com.maisonglow.exception.ValidationException;
import com.maisonglow.service.CustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.Flow;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class ApiTryOnServiceTest {
    @TempDir Path directory;
    private final CustomerService customers = mock(CustomerService.class);
    private final HttpClient client = mock(HttpClient.class);
    private ApiTryOnService service;
    private Path photo;

    @BeforeEach
    void setUp() throws Exception {
        when(client.followRedirects()).thenReturn(HttpClient.Redirect.NEVER);
        service = new ApiTryOnService(customers, client, URI.create("https://api.openai.com/v1/images/edits"),
            "gpt-image-1.5", "test-key");
        photo = directory.resolve("private-customer-name.jpg");
        ImageIO.write(new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB), "jpg", photo.toFile());
        clearInvocations(client);
    }

    private TryOnRequest request() { return new TryOnRequest(1, photo, " Rojo Cobrizo "); }

    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private String validBody() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        BufferedImage output = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        output.setRGB(10, 10, 0xABCDEF);
        ImageIO.write(output, "png", bytes);
        return "{\"data\":[{\"b64_json\":\"" + Base64.getEncoder().encodeToString(bytes.toByteArray()) + "\"}]}";
    }

    @Test
    void sendsValidatedPhotoAndReturnsProviderPixelsWithPermanentLabel() throws Exception {
        byte[] original = Files.readAllBytes(photo);
        var success = response(200, validBody());
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(success);
        TryOnResult result = service.generate(request());
        BufferedImage output = ImageIO.read(result.image().toFile());
        assertThat(output.getRGB(10, 10) & 0xFFFFFF).isEqualTo(0xABCDEF);
        assertThat(output.getHeight()).isEqualTo(356);
        assertThat(output.getRGB(200, 320) & 0xFFFFFF).isEqualTo(0x32212C);
        assertThat(result.disclaimer()).isEqualTo(SimulatedTryOnService.DISCLAIMER);
        assertThat(Files.readAllBytes(photo)).isEqualTo(original);
        var captured = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(captured.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest http = captured.getValue();
        assertThat(http.method()).isEqualTo("POST");
        assertThat(http.timeout()).contains(java.time.Duration.ofSeconds(120));
        assertThat(http.headers().firstValue("Authorization")).contains("Bearer test-key");
        String multipart = bodyOf(http);
        String boundary = http.headers().firstValue("Content-Type").orElseThrow().split("boundary=")[1];
        assertThat(multipart).startsWith("--" + boundary).endsWith("--" + boundary + "--\r\n")
            .contains("name=\"model\"\r\n\r\ngpt-image-1.5", "name=\"image\"; filename=\"photo.png\"",
                "Content-Type: image/png", "rojo cobrizo", "name=\"output_format\"\r\n\r\npng")
            .doesNotContain("private-customer-name", "test-key");
        verify(customers).findById(1);
    }

    @Test
    void rejectsInvalidInputsBeforeSendingAnyPhoto() throws Exception {
        assertThatThrownBy(() -> service.generate(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generate(new TryOnRequest(0, photo, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, photo, "unsupported")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.generate(new TryOnRequest(1, null, "rojo cobrizo")))
            .isInstanceOf(ValidationException.class);
        when(customers.findById(1)).thenThrow(new BusinessRuleException("missing"));
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(ValidationException.class);
        Files.writeString(photo, "not an image");
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(ValidationException.class);
        verifyNoInteractions(client);
    }

    @Test
    void rejectsOversizedOrUnsupportedPhotosBeforeNetwork() throws Exception {
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", photo.toFile());
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(ValidationException.class)
            .hasMessageContaining("4096");
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "gif", photo.toFile());
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(ValidationException.class);
        Files.write(photo, new byte[10 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(ValidationException.class)
            .hasMessageContaining("10 MB");
        verifyNoInteractions(client, customers);
    }

    @Test
    void rejectsOversizedProviderImageBeforePixelDecoding() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var oversized = response(200, "{\"data\":[{\"b64_json\":\""
            + Base64.getEncoder().encodeToString(bytes.toByteArray()) + "\"}]}");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(oversized);
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(DataAccessException.class)
            .hasMessageContaining("no está disponible").hasNoCause();
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "", "not json", "{}", "{\"data\":[{\"b64_json\":\"???\"}]}",
        "{\"data\":[{\"b64_json\":\"bm90IGFuIGltYWdl\"}]}"})
    void malformedProviderOutputIsControlled(String body) throws Exception {
        var malformed = response(200, body);
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(malformed);
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(DataAccessException.class)
            .hasMessageContaining("no está disponible").hasNoCause();
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 429, 500, 503})
    void providerErrorsDoNotExposeResponseAndNextRequestCanSucceed(int status) throws Exception {
        var failure = response(status, "test-key private-provider-error");
        var success = response(200, validBody());
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(failure, success);
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(DataAccessException.class)
            .hasMessageContaining("no está disponible").hasNoCause();
        assertThat(service.generate(request()).image()).exists();
    }

    @Test
    void timeoutAndInterruptionAreControlled() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new HttpTimeoutException("private"), new InterruptedException("private"));
        assertThatThrownBy(() -> service.generate(request())).isInstanceOf(DataAccessException.class).hasNoCause();
        try {
            assertThatThrownBy(() -> service.generate(request())).isInstanceOf(DataAccessException.class).hasNoCause();
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally { Thread.interrupted(); }
    }

    @Test
    void disallowsUntrustedEndpointAndRedirects() {
        assertThatThrownBy(() -> new ApiTryOnService(customers, client, URI.create("https://example.com/v1/images/edits"),
            "model", "key")).isInstanceOf(IllegalArgumentException.class);
        when(client.followRedirects()).thenReturn(HttpClient.Redirect.ALWAYS);
        assertThatThrownBy(() -> new ApiTryOnService(customers, client, URI.create("https://api.openai.com/v1/images/edits"),
            "model", "key")).isInstanceOf(IllegalArgumentException.class);
    }

    private String bodyOf(HttpRequest request) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                output.writeBytes(bytes);
            }
            @Override public void onError(Throwable error) { throw new AssertionError(error); }
            @Override public void onComplete() { }
        });
        return output.toString(StandardCharsets.UTF_8);
    }
}
