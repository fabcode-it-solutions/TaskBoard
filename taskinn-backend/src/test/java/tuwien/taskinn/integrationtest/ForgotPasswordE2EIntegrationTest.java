package tuwien.taskinn.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tuwien.taskinn.basetest.TestData.BASE_URI;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.MediaType;
import tuwien.taskinn.basetest.BaseIntegrationTest;
import tuwien.taskinn.dto.request.ForgotPasswordRequestDto;
import tuwien.taskinn.dto.request.ResetPasswordRequestDto;
import tuwien.taskinn.entity.Employee;

@DisplayName("End-to-End Test: Full Password Reset Flow")
public class ForgotPasswordE2EIntegrationTest extends BaseIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("testuser", "testpass"))
            .withPerMethodLifecycle(false);

    @Test
    @DisplayName("Full Flow: Request reset, receive email, and reset password successfully")
    void fullPasswordResetFlow_whenUserExistsAndTokenIsValid_shouldSucceed() throws Exception {
        String userEmail = "user.to.reset@test.com";
        String originalPassword = "oldPassword123";
        Employee employee = Employee.builder()
                .email(userEmail)
                .firstName("Test")
                .lastName("User")
                .passwordHash(passwordEncoder.encode(originalPassword))
                .active(true)
                .build();
        employeeRepository.save(employee);

        var forgotPasswordDto = new ForgotPasswordRequestDto(userEmail);

        mockMvc.perform(post(BASE_URI + "/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(forgotPasswordDto))).andExpect(status().isAccepted());

        greenMail.waitForIncomingEmail(5000, 1);
        MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
        assertThat(receivedMessages).hasSize(1);

        MimeMessage receivedEmail = receivedMessages[0];

        String emailBody = (String) receivedEmail.getContent();
        String resetToken = extractTokenFromEmail(emailBody);
        assertThat(resetToken).isNotNull().isNotEmpty();

        String newPassword = "newStrongPassword456!";
        var resetPasswordDto = new ResetPasswordRequestDto(resetToken, newPassword);

        mockMvc.perform(post(BASE_URI + "/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetPasswordDto))).andExpect(status().isNoContent()); // Verify
                                                                                                                // success
                                                                                                                // (204)

        Employee updatedEmployee = employeeRepository.findByEmail(userEmail).orElseThrow();
        assertAll(() -> assertThat(updatedEmployee.getPasswordHash()).isNotEqualTo(employee.getPasswordHash()),
                () -> assertThat(passwordEncoder.matches(newPassword, updatedEmployee.getPasswordHash())).isTrue(),
                () -> assertThat(passwordEncoder.matches(originalPassword, updatedEmployee.getPasswordHash())).isFalse()

        );
    }

    /** A helper utility to parse the reset token from the HTML email body. */
    private String extractTokenFromEmail(String emailBody) {
        Pattern pattern = Pattern.compile("token=([a-fA-F0-9\\-]+)");
        Matcher matcher = pattern.matcher(emailBody);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new AssertionError("Could not find reset token in the email body.");
    }
}
