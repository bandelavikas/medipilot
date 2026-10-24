package backend.call;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Passes call set-up messages between a doctor and their patient.
// It never sees or stores audio or video: that travels directly between the two browsers.
@Component
public class CallSignalingHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CallSignalingHandler.class);

    private final Gson gson = new Gson();
    private final CallAuthorizationService authorizationService;
    private final Object lock = new Object();

    private final Map<Long, WebSocketSession> online = new ConcurrentHashMap<>();
    private final Map<String, ActiveCall> calls = new ConcurrentHashMap<>();
    private final Map<Long, String> callOfUser = new ConcurrentHashMap<>();

    private static final class ActiveCall {
        private final String id;
        private final Long callerId;
        private final Long calleeId;
        private boolean accepted;

        private ActiveCall(String id, Long callerId, Long calleeId) {
            this.id = id;
            this.callerId = callerId;
            this.calleeId = calleeId;
        }

        private Long otherParty(Long userId) {
            return userId.equals(callerId) ? calleeId : callerId;
        }

        private boolean involves(Long userId) {
            return userId.equals(callerId) || userId.equals(calleeId);
        }
    }

    public CallSignalingHandler(CallAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = userIdOf(session);

        // Only one connection per user. A new one replaces the old one and ends any call.
        endCallFor(userId, "disconnected");
        WebSocketSession previous = online.put(userId, session);
        if (previous != null && previous != session && previous.isOpen()) {
            previous.close(new CloseStatus(4000, "Signed in from another tab"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userIdOf(session);
        if (online.remove(userId, session)) {
            endCallFor(userId, "disconnected");
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = userIdOf(session);

        JsonObject json;
        String type;
        try {
            json = JsonParser.parseString(message.getPayload()).getAsJsonObject();
            type = text(json, "type");
        } catch (RuntimeException e) {
            sendTo(session, error("Invalid message"));
            return;
        }
        if (type == null) {
            sendTo(session, error("Missing message type"));
            return;
        }

        synchronized (lock) {
            try {
                switch (type) {
                    case "call-request" -> handleCallRequest(session, userId, json);
                    case "call-accept" -> handleAccept(session, userId, json);
                    case "call-reject" -> handleReject(session, userId, json);
                    case "call-end" -> handleEnd(session, userId, json);
                    case "offer", "answer", "ice" -> relay(session, userId, type, json);
                    default -> sendTo(session, error("Unknown message type"));
                }
            } catch (RuntimeException e) {
                log.warn("Could not handle a call message: {}", e.toString());
                sendTo(session, error("Could not process the message"));
            }
        }
    }

    private void handleCallRequest(WebSocketSession session, Long userId, JsonObject json) {
        String media = text(json, "media");
        JsonElement toElement = json.get("to");
        if (toElement == null || !toElement.isJsonPrimitive()
                || media == null || !(media.equals("audio") || media.equals("video"))) {
            sendTo(session, error("Invalid call request"));
            return;
        }

        Long calleeId = toElement.getAsLong();
        if (calleeId.equals(userId)) {
            sendTo(session, error("You cannot call yourself"));
            return;
        }
        if (!authorizationService.isAuthorizedPair(userId, calleeId)) {
            sendTo(session, error("You can only call your own doctor or patient"));
            return;
        }
        if (callOfUser.containsKey(userId)) {
            sendTo(session, error("You are already in a call"));
            return;
        }

        WebSocketSession calleeSession = online.get(calleeId);
        if (calleeSession == null || !calleeSession.isOpen()) {
            sendTo(session, Map.of("type", "call-unavailable", "reason", "offline"));
            return;
        }
        if (callOfUser.containsKey(calleeId)) {
            sendTo(session, Map.of("type", "call-busy"));
            return;
        }

        ActiveCall call = new ActiveCall(UUID.randomUUID().toString(), userId, calleeId);
        calls.put(call.id, call);
        callOfUser.put(userId, call.id);
        callOfUser.put(calleeId, call.id);

        sendTo(calleeSession, Map.of(
                "type", "incoming-call",
                "callId", call.id,
                "from", userId,
                "fromName", nameOf(session),
                "media", media));
        sendTo(session, Map.of("type", "call-ringing", "callId", call.id));
    }

    private void handleAccept(WebSocketSession session, Long userId, JsonObject json) {
        ActiveCall call = findCall(json);
        if (call == null || !call.calleeId.equals(userId) || call.accepted) {
            sendTo(session, error("Call not found"));
            return;
        }
        call.accepted = true;
        send(call.callerId, Map.of("type", "call-accepted", "callId", call.id));
    }

    private void handleReject(WebSocketSession session, Long userId, JsonObject json) {
        ActiveCall call = findCall(json);
        if (call == null || !call.calleeId.equals(userId) || call.accepted) {
            sendTo(session, error("Call not found"));
            return;
        }
        finish(call);
        send(call.callerId, Map.of("type", "call-rejected", "callId", call.id));
    }

    private void handleEnd(WebSocketSession session, Long userId, JsonObject json) {
        ActiveCall call = findCall(json);
        if (call == null || !call.involves(userId)) {
            sendTo(session, error("Call not found"));
            return;
        }
        finish(call);
        send(call.otherParty(userId), Map.of(
                "type", "call-ended",
                "callId", call.id,
                "reason", call.accepted ? "hangup" : "cancelled"));
    }

    private void relay(WebSocketSession session, Long userId, String type, JsonObject json) {
        ActiveCall call = findCall(json);
        JsonElement data = json.get("data");
        if (call == null || !call.involves(userId) || !call.accepted || data == null) {
            sendTo(session, error("Call not found"));
            return;
        }
        send(call.otherParty(userId), Map.of("type", type, "callId", call.id, "data", data));
    }

    private void endCallFor(Long userId, String reason) {
        synchronized (lock) {
            String callId = callOfUser.get(userId);
            if (callId == null) {
                return;
            }
            ActiveCall call = calls.get(callId);
            if (call == null) {
                callOfUser.remove(userId);
                return;
            }
            finish(call);
            send(call.otherParty(userId), Map.of("type", "call-ended", "callId", callId, "reason", reason));
        }
    }

    private void finish(ActiveCall call) {
        calls.remove(call.id);
        callOfUser.remove(call.callerId, call.id);
        callOfUser.remove(call.calleeId, call.id);
    }

    private ActiveCall findCall(JsonObject json) {
        String callId = text(json, "callId");
        return callId == null ? null : calls.get(callId);
    }

    private String text(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return (element != null && element.isJsonPrimitive()) ? element.getAsString() : null;
    }

    private Long userIdOf(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }

    private String nameOf(WebSocketSession session) {
        return (String) session.getAttributes().get("fullName");
    }

    private Map<String, Object> error(String message) {
        return Map.of("type", "error", "message", message);
    }

    private void send(Long userId, Map<String, Object> payload) {
        WebSocketSession target = online.get(userId);
        if (target != null && target.isOpen()) {
            sendTo(target, payload);
        }
    }

    private void sendTo(WebSocketSession session, Map<String, Object> payload) {
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(gson.toJson(payload)));
            }
        } catch (IOException e) {
            log.warn("Could not send a call message: {}", e.toString());
        }
    }
}