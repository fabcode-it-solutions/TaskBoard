package tuwien.taskinn.services;

import tuwien.taskinn.dto.response.QrTokenResponseDto;
import tuwien.taskinn.dto.response.SignInResponseDto;

public interface MobileAuthService {

    /**
     * Generates a QR token for mobile authentication. Only employees (not admins) can generate tokens.
     *
     * @param email The email of the authenticated employee
     * @return QrTokenResponseDto containing the token, sessionId, and expiration time
     * @throws IllegalArgumentException if the user is not found or is an admin
     */
    QrTokenResponseDto generateQrToken(String email);

    /**
     * Exchanges a temporary QR token for a JWT access token. This method uses long-polling and will block until the PC
     * confirms the login, denies it, or the request times out (30 seconds).
     *
     * @param tempToken The temporary token from the QR code
     * @return SignInResponseDto containing the JWT access token
     * @throws IllegalArgumentException if the token is invalid, expired, or denied
     */
    SignInResponseDto exchangeToken(String tempToken);

    /**
     * Confirms or denies a mobile login attempt from the PC.
     *
     * @param sessionId The session ID associated with the QR token
     * @param confirmed true to approve the login, false to deny
     * @param userEmail The email of the authenticated user (for ownership verification)
     * @throws IllegalArgumentException if the sessionId is invalid
     * @throws org.springframework.security.access.AccessDeniedException if the user is not the token owner
     */
    void confirmLogin(String sessionId, boolean confirmed, String userEmail);
}
