package backend.call;

import backend.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patient")
public class PatientContactController {

    private final CallAuthorizationService authorizationService;

    public PatientContactController(CallAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/my-doctor")
    public DoctorContactResponse myDoctor(@AuthenticationPrincipal AuthenticatedUser patient) {
        return authorizationService.findDoctorContact(patient.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No doctor has been assigned to you yet"));
    }
}