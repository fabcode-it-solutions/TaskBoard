package tuwien.taskinn.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tuwien.taskinn.dto.request.QrLoginConfirmRequestDto;
import tuwien.taskinn.dto.request.QrTokenExchangeRequestDto;
import tuwien.taskinn.dto.response.QrTokenResponseDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.services.MobileAuthService;
import tuwien.taskinn.util.UserUtil;

@RestController
@RequestMapping("/api/v1/auth/mobile")
@RequiredArgsConstructor
public class MobileAuthController {

    private final MobileAuthService mobileAuthService;

    /**
     * Generate a QR token for mobile authentication. Only employees (not admins) can generate QR tokens.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/qr-token")
    public ResponseEntity<QrTokenResponseDto> generateQrToken() {
        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();

        if (activeUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        QrTokenResponseDto response = mobileAuthService.generateQrToken(activeUser.getIdentifier());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Exchange a temporary QR token for a JWT access token. This is a public endpoint called by the mobile device. Uses
     * long-polling to wait for PC confirmation.
     */
    @PostMapping("/exchange")
    public ResponseEntity<SignInResponseDto> exchangeToken(@Valid @RequestBody QrTokenExchangeRequestDto request) {
        SignInResponseDto response = mobileAuthService.exchangeToken(request.getToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * Confirm or deny a mobile login attempt from the PC.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/confirm")
    public ResponseEntity<Void> confirmLogin(@Valid @RequestBody QrLoginConfirmRequestDto request) {
        UserUtil.UserInfo activeUser = UserUtil.getActiveUser();

        if (activeUser == null) {
            throw new AccessDeniedException("User is not authenticated");
        }

        mobileAuthService.confirmLogin(request.getSessionId(), request.isConfirmed(), activeUser.getIdentifier());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
