package backend.call;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Optional;

// Runs before a WebSocket connection is accepted. No valid one-time ticket, no connection.
public class TicketHandshakeInterceptor implements HandshakeInterceptor {

    private final CallTicketService ticketService;

    public TicketHandshakeInterceptor(CallTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("ticket");

        Optional<CallTicketService.Ticket> redeemed = ticketService.redeem(ticket);
        if (redeemed.isEmpty()) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }

        attributes.put("userId", redeemed.get().userId());
        attributes.put("fullName", redeemed.get().fullName());
        attributes.put("role", redeemed.get().role());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Nothing to do after the handshake.
    }
}