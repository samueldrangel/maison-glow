package com.maisonglow.ai;

import com.maisonglow.exception.DataAccessException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Text persistence for the four AI tables in sql/schema.sql. */
public final class AiHistory {
    public record Conversation(int id, int userId, LocalDateTime startedAt) { }
    public record Message(int id, int conversationId, Sender sender, String content, LocalDateTime createdAt) { }
    public record ToolCall(int id, int messageId, String toolName, String arguments, String result,
                           boolean requiresConfirmation, ToolStatus status, LocalDateTime executedAt) { }
    public record Session(int id, int customerId, Path sourceImagePath, String transformation,
                          Path resultImagePath, SessionStatus status, String provider,
                          String errorMessage, LocalDateTime createdAt) { }
    public enum Sender { USER, ASSISTANT }
    public enum ToolStatus { PENDING_CONFIRMATION, EXECUTED, REJECTED, FAILED }
    public enum SessionStatus { PENDING, COMPLETED, FAILED }

    private final Clock clock;
    private final com.maisonglow.service.AuthenticationService auth;
    private final com.maisonglow.dao.CommerceDao consent;
    private final AiTextTable<Conversation> conversations;
    private final AiTextTable<Message> messages;
    private final AiTextTable<ToolCall> tools;
    private final AiTextTable<Session> sessions;

    /** Creates the four tables in the same data folder used by the business DAOs. */
    public AiHistory(Path folder, Clock clock) {
        this(folder,clock,null);
    }

    /** Production history is account-scoped; the two-argument constructor supports legacy imports/tests only. */
    public AiHistory(Path folder,Clock clock,com.maisonglow.service.AuthenticationService auth) {
        this(folder,clock,auth,null);
    }
    public AiHistory(Path folder,Clock clock,com.maisonglow.service.AuthenticationService auth,com.maisonglow.dao.CommerceDao consent) {
        Objects.requireNonNull(folder, "folder");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.auth=auth;
        this.consent=consent;
        conversations = new AiTextTable<>(folder.resolve("assistant_conversations.txt"),
            f -> { count(f, 3); return new Conversation(id(f[0]), id(f[1]), LocalDateTime.parse(f[2])); },
            c -> new String[] {str(c.id()), str(c.userId()), c.startedAt().toString()});
        messages = new AiTextTable<>(folder.resolve("assistant_messages.txt"),
            f -> { count(f, 5); return new Message(id(f[0]), id(f[1]), Sender.valueOf(f[2]), f[3], LocalDateTime.parse(f[4])); },
            m -> new String[] {str(m.id()), str(m.conversationId()), m.sender().name(), m.content(), m.createdAt().toString()});
        tools = new AiTextTable<>(folder.resolve("assistant_tool_calls.txt"),
            f -> { count(f, 8); return new ToolCall(id(f[0]), id(f[1]), f[2], f[3], blankToNull(f[4]),
                "1".equals(f[5]), ToolStatus.valueOf(f[6]), f[7].isEmpty() ? null : LocalDateTime.parse(f[7])); },
            t -> new String[] {str(t.id()), str(t.messageId()), t.toolName(), t.arguments(), value(t.result()),
                t.requiresConfirmation() ? "1" : "0", t.status().name(),
                t.executedAt() == null ? "" : t.executedAt().toString()});
        sessions = new AiTextTable<>(folder.resolve("try_on_sessions.txt"),
            f -> { count(f, 9); return new Session(id(f[0]), id(f[1]), Path.of(f[2]), f[3],
                f[4].isEmpty() ? null : Path.of(f[4]), SessionStatus.valueOf(f[5]), f[6],
                blankToNull(f[7]), LocalDateTime.parse(f[8])); },
            s -> new String[] {str(s.id()), str(s.customerId()), s.sourceImagePath().toString(), s.transformation(),
                s.resultImagePath() == null ? "" : s.resultImagePath().toString(), s.status().name(),
                s.provider(), value(s.errorMessage()), s.createdAt().toString()});
    }

    /** Begins a conversation for the user id reserved by the future app_user table. */
    public Conversation startConversation(int userId) throws DataAccessException {
        requireUser(userId);
        positive(userId);
        return conversations.append(id -> new Conversation(id, userId, now()));
    }

    /** Persists one user or assistant message, including newlines and separator characters. */
    public Message addMessage(int conversationId, Sender sender, String content) throws DataAccessException {
        requireConversation(conversationId);
        positive(conversationId);
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(content, "content");
        if (content.isBlank()) throw new IllegalArgumentException("El mensaje está vacío.");
        if (conversations.all().stream().noneMatch(c -> c.id() == conversationId))
            throw new DataAccessException("La conversación no existe.");
        return messages.append(id -> new Message(id, conversationId, sender, content, now()));
    }

    /** Records a whitelisted tool request or result under an assistant message. */
    public ToolCall addToolCall(int messageId, String toolName, String arguments, String result,
                                boolean requiresConfirmation, ToolStatus status, LocalDateTime executedAt)
        throws DataAccessException {
        requireMessage(messageId);
        positive(messageId);
        Objects.requireNonNull(toolName, "toolName");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(status, "status");
        if (messages.all().stream().noneMatch(m -> m.id() == messageId && m.sender() == Sender.ASSISTANT))
            throw new DataAccessException("El mensaje del asistente no existe.");
        return tools.append(id -> new ToolCall(id, messageId, toolName, arguments, result,
            requiresConfirmation, status, executedAt));
    }

    /** Updates the audit entry after the user confirms or rejects a proposed action. */
    public ToolCall finishToolCall(int id, ToolStatus status, String result) throws DataAccessException {
        Objects.requireNonNull(status, "status");
        if (status == ToolStatus.PENDING_CONFIRMATION) throw new IllegalArgumentException("La llamada sigue pendiente.");
        ToolCall old = tools.all().stream().filter(t -> t.id() == id).findFirst()
            .orElseThrow(() -> new DataAccessException("La llamada no existe."));
        requireMessage(old.messageId());
        return tools.update(id, new ToolCall(id, old.messageId(), old.toolName(), old.arguments(), result,
            old.requiresConfirmation(), status, status == ToolStatus.EXECUTED ? now() : null));
    }

    /** Begins a try-on session; the source path is metadata, not copied image data. */
    public Session startSession(int customerId, Path photo, String transformation, String provider)
        throws DataAccessException {
        requireCustomer(customerId);
        positive(customerId);
        Objects.requireNonNull(photo, "photo");
        Objects.requireNonNull(transformation, "transformation");
        Objects.requireNonNull(provider, "provider");
        return sessions.append(id -> new Session(id, customerId, photo, transformation,
            null, SessionStatus.PENDING, provider, null, now()));
    }

    /** Saves the final result or controlled failure of a session. */
    public Session finishSession(int id, Path image, String error) throws DataAccessException {
        Session old = sessions.all().stream().filter(s -> s.id() == id).findFirst()
            .orElseThrow(() -> new DataAccessException("La sesión no existe."));
        requireCustomer(old.customerId());
        if ((image == null) == (error == null)) throw new IllegalArgumentException("Indica imagen o error.");
        return sessions.update(id, new Session(id, old.customerId(), old.sourceImagePath(), old.transformation(),
            image, image == null ? SessionStatus.FAILED : SessionStatus.COMPLETED,
            old.provider(), error, old.createdAt()));
    }

    public List<Conversation> conversations() throws DataAccessException {
        if(auth==null)return conversations.all();
        int user=currentUser();return conversations.all().stream().filter(c -> c.userId()==user).toList();
    }
    public List<Message> messages(int conversationId) throws DataAccessException {
        requireConversation(conversationId);
        return messages.all().stream().filter(m -> m.conversationId() == conversationId).toList();
    }
    public List<ToolCall> toolCalls(int messageId) throws DataAccessException {
        requireMessage(messageId);
        return tools.all().stream().filter(t -> t.messageId() == messageId).toList();
    }
    public List<Session> sessions() throws DataAccessException {
        if(auth==null)return sessions.all();
        currentUser();
        java.util.ArrayList<Session> result=new java.util.ArrayList<>();
        for(var session:sessions.all()) {try{requireCustomer(session.customerId());result.add(session);}catch(com.maisonglow.exception.AccessDeniedDataException ignored){}}
        return List.copyOf(result);
    }

    private int currentUser()throws DataAccessException {
        try{return auth.requireAuthenticated().id();}
        catch(com.maisonglow.exception.AuthorizationException e){throw new com.maisonglow.exception.AccessDeniedDataException(e.getMessage(),e);}
    }
    private void requireUser(int id)throws DataAccessException {if(auth!=null&&currentUser()!=id)throw new com.maisonglow.exception.AccessDeniedDataException("La conversación no pertenece a tu cuenta.",null);}
    private void requireConversation(int id)throws DataAccessException {
        if(auth!=null) {currentUser();var conversation=conversations.all().stream().filter(c -> c.id()==id).findFirst().orElseThrow(() -> new DataAccessException("La conversación no existe."));requireUser(conversation.userId());}
    }
    private void requireMessage(int id)throws DataAccessException {
        if(auth!=null) {currentUser();var message=messages.all().stream().filter(m -> m.id()==id).findFirst().orElseThrow(() -> new DataAccessException("El mensaje no existe."));requireConversation(message.conversationId());}
    }
    private void requireCustomer(int id)throws DataAccessException {
        if(auth!=null)try{
            auth.requireCustomerAccess(id);
            if(auth.requireAuthenticated().role()==com.maisonglow.model.UserRole.ADMIN&&(consent==null||!consent.staffAccess(id)))
                throw new com.maisonglow.exception.AuthorizationException("El cliente no ha autorizado el historial de fotos.");
        }catch(com.maisonglow.exception.AuthorizationException e){throw new com.maisonglow.exception.AccessDeniedDataException(e.getMessage(),e);}
    }

    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private static void positive(int id) { if (id < 1) throw new IllegalArgumentException("El id debe ser positivo."); }
    private static int id(String field) { int id = Integer.parseInt(field); positive(id); return id; }
    private static String str(int number) { return Integer.toString(number); }
    private static String value(String text) { return text == null ? "" : text; }
    private static String blankToNull(String text) { return text.isEmpty() ? null : text; }
    private static void count(String[] fields, int expected) {
        if (fields.length != expected) throw new IllegalArgumentException("Cantidad de campos incorrecta.");
    }
}
