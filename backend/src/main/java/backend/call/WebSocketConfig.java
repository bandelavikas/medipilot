package backend.call;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CallSignalingHandler signalingHandler;
    private final CallTicketService ticketService;

    public WebSocketConfig(CallSignalingHandler signalingHandler, CallTicketService ticketService) {
        this.signalingHandler = signalingHandler;
        this.ticketService = ticketService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingHandler, "/ws/calls")
                .addInterceptors(new TicketHandshakeInterceptor(ticketService))
                .setAllowedOrigins("http://localhost:5173");
    }

    // Call set-up messages can be a few kilobytes. This allows up to 64 KB and no more.
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(64 * 1024);
        container.setMaxBinaryMessageBufferSize(64 * 1024);
        return container;
    }
}