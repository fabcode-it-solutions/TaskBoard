package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.annotation.RequireRole;
import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.services.AuthService;

@RequireRole(Roles.ADMIN)
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @PostMapping("/account")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequestDto req) {
        authService.signUpEmployee(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }
}
