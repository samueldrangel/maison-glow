package com.maisonglow.ai;

import com.maisonglow.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ToolRegistryTest {
    private AssistantTool tool(boolean confirmation) throws Exception {
        AssistantTool tool = mock(AssistantTool.class);
        when(tool.getName()).thenReturn("test_tool");
        when(tool.getDescription()).thenReturn("Operación de prueba");
        when(tool.getParameters()).thenReturn(Map.of("value", "Valor"));
        when(tool.requiresConfirmation()).thenReturn(confirmation);
        when(tool.execute(anyMap())).thenReturn("Listo");
        return tool;
    }

    @Test
    void rejectsUnknownToolsAndDuplicateNames() throws Exception {
        AssistantTool tool = tool(false);
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        assertThatThrownBy(() -> registry.execute("delete_all", Map.of()))
            .isInstanceOf(ValidationException.class).hasMessageContaining("no registrada");
        assertThatThrownBy(() -> new ToolRegistry(List.of(tool, tool)))
            .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("duplicada");
        verify(tool, never()).execute(anyMap());
    }

    @Test
    void queriesExecuteImmediatelyWithoutConfirmation() throws Exception {
        AssistantTool tool = tool(false);
        AssistantReply result = new ToolRegistry(List.of(tool)).execute("test_tool", Map.of("value", "1"));
        assertThat(result.text()).isEqualTo("Listo");
        assertThat(result.pendingConfirmation()).isEmpty();
        verify(tool).execute(Map.of("value", "1"));
    }

    @Test
    void confirmationUsesOriginalSnapshotAndExecutesOnlyOnce() throws Exception {
        AssistantTool tool = tool(true);
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        Map<String, String> input = new HashMap<>(Map.of("value", "1"));
        PendingAction action = registry.execute("test_tool", input).pendingConfirmation().orElseThrow();
        input.put("value", "2");
        verify(tool, never()).execute(anyMap());
        assertThatThrownBy(() -> action.parameters().put("value", "3"))
            .isInstanceOf(UnsupportedOperationException.class);
        assertThat(registry.confirm(action).text()).isEqualTo("Listo");
        assertThatThrownBy(() -> registry.confirm(action)).isInstanceOf(ValidationException.class);
        verify(tool, times(1)).execute(Map.of("value", "1"));
    }

    @Test
    void rejectionAndForeignOrModifiedActionsNeverExecute() throws Exception {
        AssistantTool tool = tool(true);
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        PendingAction action = registry.execute("test_tool", Map.of("value", "1"))
            .pendingConfirmation().orElseThrow();
        assertThatThrownBy(() -> registry.confirm(new PendingAction(action.id(), action.toolName(), Map.of("value", "2"))))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new ToolRegistry(List.of(tool)).confirm(action))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> registry.confirm(new PendingAction(UUID.randomUUID(), "test_tool", Map.of())))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> registry.confirm(null)).isInstanceOf(ValidationException.class);
        assertThat(registry.reject(action).text()).contains("cancelada");
        assertThatThrownBy(() -> registry.confirm(action)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> registry.reject(action)).isInstanceOf(ValidationException.class);
        verify(tool, never()).execute(anyMap());
    }

    @Test
    void rejectsUnknownBlankAndNullParameters() throws Exception {
        AssistantTool tool = tool(false);
        ToolRegistry registry = new ToolRegistry(List.of(tool));
        assertThatThrownBy(() -> registry.execute("test_tool", Map.of("other", "1")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> registry.execute("test_tool", Map.of("value", " ")))
            .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> registry.execute("test_tool", null))
            .isInstanceOf(ValidationException.class);
        Map<String, String> nullName = new HashMap<>();
        nullName.put(null, "1");
        assertThatThrownBy(() -> registry.execute("test_tool", nullName))
            .isInstanceOf(ValidationException.class);
        verify(tool, never()).execute(anyMap());
    }
}
