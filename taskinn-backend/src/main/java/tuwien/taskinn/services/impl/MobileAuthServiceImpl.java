package tuwien.taskinn.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tuwien.taskinn.exceptions.TokenExpiredException;
import tuwien.taskinn.dto.response.QrLoginAttemptDto;
import tuwien.taskinn.dto.response.QrTokenResponseDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.entity.Employee;
import tuwien.taskinn.entity.MobileAuthToken;
import tuwien.taskinn.enums.MobileAuthTokenStatus;
import tuwien.taskinn.enums.Roles;
import tuwien.taskinn.repo.EmployeeRepository;
import tuwien.taskinn.repo.MobileAuthTokenRepository;
import tuwien.taskinn.services.JwtService;
import tuwien.taskinn.services.MobileAuthService;

@Service
@RequiredArgsConstructor
public class MobileAuthServiceImpl implements MobileAuthService {

    private final MobileAuthTokenRepository mobileAuthTokenRepository;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.qr.token-ttl-seconds:60}")
    private long tokenTtlSeconds;

    @Value("${app.qr.exchange-timeout-seconds:30}")
    private long exchangeTimeoutSeconds;

    @Override
    @Transactional
    public QrTokenResponseDto generateQrToken(String email) {
        Employee employee =
                employeeRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (employee.getRoles().contains(Roles.ADMIN)) {
            throw new IllegalArgumentException("Admins cannot use QR code login");
        }

        String token = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();

        MobileAuthToken mobileAuthToken = MobileAuthToken.builder()
                .token(token)
                .sessionId(sessionId)
                .employee(employee)
                .expiresAt(Instant.now().plus(tokenTtlSeconds, ChronoUnit.SECONDS))
                .status(MobileAuthTokenStatus.PENDING)
                .build();

        mobileAuthTokenRepository.save(mobileAuthToken);

        return QrTokenResponseDto.builder().token(token).sessionId(sessionId).expiresInSeconds(tokenTtlSeconds).build();
    }

    @Override
    public SignInResponseDto exchangeToken(String tempToken) {
        // Find and validate the token
        MobileAuthToken mobileAuthToken = mobileAuthTokenRepository.findByToken(tempToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        // Check if token is expired
        if (mobileAuthToken.getExpiresAt().isBefore(Instant.now())) {
            updateTokenStatus(mobileAuthToken.getId(), MobileAuthTokenStatus.EXPIRED);
            throw new IllegalArgumentException("Token expired");
        }

        // Check if token was already used
        if (mobileAuthToken.getUsedAt() != null) {
            throw new IllegalArgumentException("Token already used");
        }

        // Check current status
        if (mobileAuthToken.getStatus() != MobileAuthTokenStatus.PENDING) {
            throw new IllegalArgumentException("Token in invalid state");
        }

        // Set status to awaiting confirmation
        updateTokenStatus(mobileAuthToken.getId(), MobileAuthTokenStatus.AWAITING_CONFIRMATION);

        // Send WebSocket notification to PC
        messagingTemplate.convertAndSend("/topic/qr-auth/" + mobileAuthToken.getSessionId(),
                QrLoginAttemptDto.builder()
                        .sessionId(mobileAuthToken.getSessionId())
                        .message("A device is trying to login")
                        .build());

        // Long-polling: wait for confirmation
        long startTime = System.currentTimeMillis();
        long timeoutMs = exchangeTimeoutSeconds * 1000;

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            entityManager.clear();
            // Re-fetch the token to check for status changes
            MobileAuthToken currentToken = mobileAuthTokenRepository.findById(mobileAuthToken.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Token not found"));

            // Check if token expired during waiting
            if (currentToken.getExpiresAt().isBefore(Instant.now())) {
                updateTokenStatus(currentToken.getId(), MobileAuthTokenStatus.EXPIRED);
                throw new IllegalArgumentException("Token expired");
            }

            if (currentToken.getStatus() == MobileAuthTokenStatus.CONFIRMED) {
                // Generate JWT and return
                Employee employee = currentToken.getEmployee();
                String jwt = jwtService.generateAccessToken(employee.getEmail(), employee.getRoles());
                return new SignInResponseDto(jwt, "Bearer", jwtService.getAccessTtlSeconds());
            }

            if (currentToken.getStatus() == MobileAuthTokenStatus.DENIED) {
                throw new IllegalArgumentException("Login denied by user");
            }

            // Sleep briefly before checking again
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalArgumentException("Token exchange interrupted");
            }
        }

        // Timeout - update status and throw
        updateTokenStatus(mobileAuthToken.getId(), MobileAuthTokenStatus.EXPIRED);
        throw new IllegalArgumentException("Login confirmation timed out");
    }

    @Override
    @Transactional(noRollbackFor = TokenExpiredException.class)
    public void confirmLogin(String sessionId, boolean confirmed, String userEmail) {
        MobileAuthToken mobileAuthToken = mobileAuthTokenRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid session ID"));

        // Verify the authenticated user owns this token
        if (!mobileAuthToken.getEmployee().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Not authorized to confirm this session");
        }

        // Check if token is expired
        if (mobileAuthToken.getExpiresAt().isBefore(Instant.now())) {
            mobileAuthToken.setStatus(MobileAuthTokenStatus.EXPIRED);
            mobileAuthTokenRepository.save(mobileAuthToken);
            throw new TokenExpiredException("Token expired");
        }

        // Check if token is in awaiting confirmation state
        if (mobileAuthToken.getStatus() != MobileAuthTokenStatus.AWAITING_CONFIRMATION) {
            throw new IllegalArgumentException("Token not awaiting confirmation");
        }

        if (confirmed) {
            mobileAuthToken.setStatus(MobileAuthTokenStatus.CONFIRMED);
            mobileAuthToken.setUsedAt(Instant.now());
        } else {
            mobileAuthToken.setStatus(MobileAuthTokenStatus.DENIED);
        }

        mobileAuthTokenRepository.save(mobileAuthToken);
    }

    @Transactional
    protected void updateTokenStatus(Long tokenId, MobileAuthTokenStatus status) {
        MobileAuthToken token = mobileAuthTokenRepository.findById(tokenId)
                .orElseThrow(() -> new IllegalArgumentException("Token not found"));
        token.setStatus(status);
        mobileAuthTokenRepository.save(token);
    }
}
