package tuwien.taskinn.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInGuestDto {
    @NotNull
    private Long roomId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @PastOrPresent
    private LocalDate dateOfBirth;

    @Email
    private String email;
    private String phoneNumber;
}
