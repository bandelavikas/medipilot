package backend.call;

import backend.security.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CallTicketService {

    private static final long TICKET_LIFETIME_SECONDS = 30;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public record Ticket(Long userId, String fullName, String role, Instant expiresAt) {
    }

    public String issue(AuthenticatedUser user) {
        purgeExpired();

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        tickets.put(ticket, new Ticket(
                user.id(),
                user.fullName(),
                user.role().name(),
                Instant.now().plusSeconds(TICKET_LIFETIME_SECONDS)));
        return ticket;
    }

    // A ticket works only once: it is removed as soon as it is used.
    public Optional<Ticket> redeem(String ticket) {
        if (ticket == null) {
            return Optional.empty();
        }
        Ticket found = tickets.remove(ticket);
        if (found == null || found.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(found);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        tickets.values().removeIf(ticket -> ticket.expiresAt().isBefore(now));
    }
}