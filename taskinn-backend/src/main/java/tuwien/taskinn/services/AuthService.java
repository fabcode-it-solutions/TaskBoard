package tuwien.taskinn.services;

import tuwien.taskinn.dto.request.SignUpRequestDto;
import tuwien.taskinn.dto.response.SignInResponseDto;
import tuwien.taskinn.dto.response.UserProfileDto;

public interface AuthService {

    /**
     * Registers a new user in the system. Creates a new user account with the provided credentials and assigns a
     * default role. The password will be securely hashed before being stored.
     *
     * @param email The email address for the new user. Must be unique.
     * @param password The plain-text password for the new user.
     * @throws IllegalArgumentException if the provided email is already registered.
     */
    void signUp(String email, String password);

    /**
     * Registers a new user in the system. Creates a new admin or employee account with the provided credentials and
     * assigns the chosen role.
     *
     * @param signUpRequestDto The DTO containing the admin's registration details.
     */
    void signUpEmployee(SignUpRequestDto signUpRequestDto);

    /**
     * Authenticates a user with their credentials and issues a JSON Web Token (JWT).
     *
     * @param email The user's registered email address.
     * @param password The user's plain-text password.
     * @return A {@link SignInResponseDto} containing the JWT access token and its metadata upon successful
     *         authentication.
     * @throws IllegalArgumentException if the provided email or password is invalid.
     */
    SignInResponseDto signIn(String email, String password);

    /**
     * Invalidates a user's current session by revoking their JWT. The token will be added to a revocation list and will
     * no longer be accepted for future authenticated requests. This method is idempotent and will not throw an error
     * for an already invalid or revoked token.
     *
     * @param token The JWT access token (without the 'Bearer ' prefix) to be revoked.
     */
    void signOut(String token);

    /**
     * Initiates the password reset process for a given email address. Generates a secure, single-use token and links it
     * to the user's account. For security, this method will not indicate whether the email was found in the system.
     *
     * <p>
     * Once the token is generated, an email is sent to the user containing the new token and instructions for resetting
     * their password.
     *
     * @param email The email address of the user requesting a password reset.
     */
    void forgotPassword(String email);

    /**
     * Resets a user's password using a valid, non-expired password reset token. After a successful reset, the token is
     * invalidated to prevent reuse.
     *
     * @param token The single-use password reset token received by the user.
     * @param newPassword The new plain-text password for the user.
     * @throws IllegalArgumentException if the token is invalid, expired, or has already been used.
     */
    void resetPassword(String token, String newPassword);

    /**
     * Retrieves the profile information of a user by their email address.
     *
     * @param email The email address of the user whose profile is to be retrieved.
     * @return A {@link UserProfileDto} containing the user's profile information.
     */
    UserProfileDto getUserProfile(String email);
}
