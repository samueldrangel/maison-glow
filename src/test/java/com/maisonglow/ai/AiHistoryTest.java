package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.assertj.core.api.Assertions.*;

class AiHistoryTest {
    @TempDir Path folder;
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void conversationMessagesAndToolAuditReloadExactly() throws Exception {
        AiHistory first = new AiHistory(folder, clock);
        var conversation = first.startConversation(7);
        var user = first.addMessage(conversation.id(), AiHistory.Sender.USER, "Hola|Ana\n¿cita?");
        var assistant = first.addMessage(conversation.id(), AiHistory.Sender.ASSISTANT, "Puedo agendarla.");
        var pending = first.addToolCall(assistant.id(), "book_appointment", "{\"day\":\"lunes\"}",
            null, true, AiHistory.ToolStatus.PENDING_CONFIRMATION, null);
        var executed = first.finishToolCall(pending.id(), AiHistory.ToolStatus.EXECUTED, "Cita agendada");

        AiHistory reopened = new AiHistory(folder, clock);
        assertThat(reopened.conversations()).containsExactly(conversation);
        assertThat(reopened.messages(conversation.id())).containsExactly(user, assistant);
        assertThat(reopened.toolCalls(assistant.id())).containsExactly(executed);
        assertThat(executed.executedAt()).isNotNull();
        assertThat(reopened.toolCalls(user.id())).isEmpty();
        assertThat(Files.readAllLines(folder.resolve("assistant_messages.txt"))).hasSize(3);
        assertThat(reopened.startConversation(8).id()).isEqualTo(conversation.id() + 1);
    }

    @Test
    void tryOnSessionReloadsAfterSuccessAndFailure() throws Exception {
        AiHistory history = new AiHistory(folder, clock);
        Path photo = folder.resolve("client.png");
        var pending = history.startSession(11, photo, "rojo cobrizo", "API");
        assertThat(pending.status()).isEqualTo(AiHistory.SessionStatus.PENDING);
        var completed = history.finishSession(pending.id(), folder.resolve("result.png"), null);
        var failedPending = history.startSession(12, photo, "rubio dorado", "SIMULATED");
        var failed = history.finishSession(failedPending.id(), null, "No se pudo generar");
        assertThat(new AiHistory(folder, clock).sessions()).containsExactly(completed, failed);
        assertThat(completed.createdAt()).isEqualTo(pending.createdAt());
        assertThat(failed.status()).isEqualTo(AiHistory.SessionStatus.FAILED);
        assertThat(new AiHistory(folder, clock).startSession(13, photo, "castaño oscuro", "API").id())
            .isEqualTo(failed.id() + 1);
    }

    @Test
    void rejectsMissingParentsAndBrokenRecords() throws Exception {
        AiHistory history = new AiHistory(folder, clock);
        assertThatThrownBy(() -> history.addMessage(99, AiHistory.Sender.USER, "hola"))
            .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> history.addToolCall(99, "lookup", "{}", null, false,
            AiHistory.ToolStatus.EXECUTED, null)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> history.finishSession(99, folder.resolve("x.png"), null))
            .isInstanceOf(DataAccessException.class);
        Files.writeString(folder.resolve("assistant_conversations.txt"), "#nextId=2\n1|broken\n");
        assertThatThrownBy(history::conversations).isInstanceOf(DataAccessException.class)
            .hasMessageContaining("línea 2");
    }
}
