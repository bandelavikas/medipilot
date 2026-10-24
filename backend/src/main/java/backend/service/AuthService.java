package backend.service;

import backend.dto.AuthResponse;
import backend.dto.LoginRequest;
import backend.dto.RegisterRequest;
import backend.entity.DoctorProfile;
import backend.entity.PatientProfile;
import backend.entity.Role;
import backend.entity.User;
import backend.repository.DoctorProfileRepository;
import backend.repository.PatientProfileRepository;
import backend.repository.UserRepository;
import backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       DoctorProfileRepository doctorProfileRepository,
                       PatientProfileRepository patientProfileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.patientProfileRepository = patientProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Admin accounts cannot be created through registration");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        userRepository.save(user);

        if (request.role() == Role.DOCTOR) {
            DoctorProfile profile = new DoctorProfile();
            profile.setUser(user);
            profile.setSpecialization(request.specialization());
            profile.setPhone(request.phone());
            doctorProfileRepository.save(profile);
        } else {
            PatientProfile profile = new PatientProfile();
            profile.setUser(user);
            profile.setDateOfBirth(request.dateOfBirth());
            profile.setPhone(request.phone());
            patientProfileRepository.save(profile);
        }

        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidLogin);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw invalidLogin();
        }

        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account has been disabled");
        }

        return buildResponse(user);
    }

    private ResponseStatusException invalidLogin() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}