package backend.call;

import backend.entity.Role;
import backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/calls")
public class CallTicketController {

    private final CallTicketService ticketService;

    public CallTicketController(CallTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/ticket")
    public CallTicketResponse ticket(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only doctors and patients can use calls");
        }
        return new CallTicketResponse(ticketService.issue(user));
    }
}