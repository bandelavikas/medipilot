package backend.controller;

import backend.dto.AssignPatientRequest;
import backend.dto.PatientSummary;
import backend.security.AuthenticatedUser;
import backend.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/patients")
public class DoctorPatientController {

    private final PatientService patientService;

    public DoctorPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public List<PatientSummary> list(@AuthenticationPrincipal AuthenticatedUser doctor) {
        return patientService.listPatientsForDoctor(doctor.id());
    }

    @PostMapping
    public ResponseEntity<PatientSummary> assign(@AuthenticationPrincipal AuthenticatedUser doctor,
                                                 @Valid @RequestBody AssignPatientRequest request) {
        PatientSummary created = patientService.assignPatient(doctor.id(), request.email());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}