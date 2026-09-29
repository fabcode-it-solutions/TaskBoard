package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.dto.request.ForgotPasswordRequestDto;
import tuwien.taskinn.dto.request.ResetPasswordRequestDto;
import tuwien.taskinn.dto.request.SignInRequestDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.dto.response.UserProfileDto;
import tuwien.taskinn.services.AuthService;
import tuwien.taskinn.util.UserUtil;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-in")
    public ResponseEntity<SignInResponseDto> signIn(@Valid @RequestBody SignInRequestDto req) {
        SignInResponseDto res = authService.signIn(req.getEmail(), req.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            authService.signOut(token);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDto req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(null);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDto req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile() {

        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();

        if (activeUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        UserProfileDto profile = authService.getUserProfile(activeUser.getIdentifier());

        return ResponseEntity.status(HttpStatus.OK).body(profile);
    }
}
