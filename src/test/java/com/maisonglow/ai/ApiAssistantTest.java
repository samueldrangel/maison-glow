package com.maisonglow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApiAssistantTest {
    private final HttpClient client = mock(HttpClient.class);
    private final AssistantTool tool = mock(AssistantTool.class);
    private final ObjectMapper json = new ObjectMapper();
    private ToolRegistry registry;
    private ApiAssistant assistant;

    @BeforeEach
    void setUp() {
        when(tool.getName()).thenReturn("find_customer");
        when(tool.getDescription()).thenReturn("Buscar cliente");
        when(tool.getParameters()).thenReturn(Map.of("name", "Nombre"));
        registry = new ToolRegistry(List.of(tool));
        assistant = new ApiAssistant(client, URI.create("https://api.openai.com/v1/responses"),
            "gpt-5.4", "test-key", registry);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> result = mock(HttpResponse.class);
        when(result.statusCode()).thenReturn(status);
        when(result.body()).thenReturn(body);
        return result;
    }

    private String message(String text) {
        return "{\"id\":\"resp_1\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\""
            + text + "\"}]}]}";
    }

    private String functionCall(String name) {
        return "{\"id\":\"resp_1\",\"output\":[{\"type\":\"function_call\",\"call_id\":\"call_1\",\"name\":\""
            + name + "\",\"arguments\":\"{\\\"name\\\":\\\"Ana\\\"}\"}]}";
    }

    @Test
    void sendsTextAndWhitelistedToolDefinitions() throws Exception {
        HttpResponse<String> first = response(200, message("Hola Ana"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(first);
        assertThat(assistant.reply("Hola").text()).isEqualTo("Hola Ana");
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture(), any(HttpResponse.BodyHandler.class));
        assertThat(request.getValue().headers().firstValue("Authorization")).contains("Bearer test-key");
        assertThat(request.getValue().timeout()).contains(java.time.Duration.ofSeconds(15));
        String body = bodyOf(request.getValue());
        assertThat(json.readTree(body).path("tools").get(0).path("name").asText()).isEqualTo("find_customer");
        assertThat(body).contains("Buscar cliente", "Nombre", "Hola");
    }

    @Test
    void executesKnownReadToolAndReturnsFollowUpText() throws Exception {
        when(tool.execute(Map.of("name", "Ana"))).thenReturn("[1] Ana");
        HttpResponse<String> first = response(200, functionCall("find_customer"));
        HttpResponse<String> second = response(200, message("Encontré a Ana"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(first, second);
        assertThat(assistant.reply("Busca a Ana").text()).isEqualTo("Encontré a Ana");
        var request = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(2)).send(request.capture(), any(HttpResponse.BodyHandler.class));
        String followUp = bodyOf(request.getAllValues().get(1));
        assertThat(followUp).contains("function_call_output", "call_1", "[1] Ana", "resp_1");
        verify(tool).execute(Map.of("name", "Ana"));
    }

    @Test
    void unknownToolIsRejectedWithoutExecutingAnything() throws Exception {
        HttpResponse<String> first = response(200, functionCall("delete_all"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(first);
        assertThat(assistant.reply("Borra todo").text()).contains("no registrada");
        verify(tool, never()).execute(anyMap());
        verify(client, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void modifyingToolWaitsForConfirmationAndRunsOnce() throws Exception {
        when(tool.requiresConfirmation()).thenReturn(true);
        when(tool.execute(Map.of("name", "Ana"))).thenReturn("Cita registrada");
        HttpResponse<String> first = response(200, functionCall("find_customer"));
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(first);
        PendingAction action = assistant.reply("Agenda a Ana").pendingConfirmation().orElseThrow();
        verify(tool, never()).execute(anyMap());
        assertThat(assistant.confirm(action).text()).isEqualTo("Cita registrada");
        assertThat(assistant.confirm(action).text()).contains("no está pendiente");
        verify(tool, times(1)).execute(Map.of("name", "Ana"));
    }

    @Test
    void timeoutAndHttpErrorReturnControlledReply() throws Exception {
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new HttpTimeoutException("secret host"));
        assertThat(assistant.reply("Hola").text()).contains("no está disponible").doesNotContain("secret host");
        reset(client);
        HttpResponse<String> unavailable = response(503, "secret host");
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(unavailable);
        assertThat(assistant.reply("Hola").text()).contains("no está disponible").doesNotContain("secret host");
    }

    private String bodyOf(HttpRequest request) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
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
