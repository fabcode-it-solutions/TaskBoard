package tuwien.taskinn.unittest.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tuwien.taskinn.exceptions.InfeasibleInstanceException;

@DisplayName("InfeasibleInstanceException Unit Tests")
public class InfeasibleInstanceExceptionTest {

    @Test
    @DisplayName("constructor with message should set the message correctly")
    public void constructor_withMessage_shouldSetCorrectly() {
        String expectedMessage = "The scheduling instance has no feasible solution";

        InfeasibleInstanceException exception = new InfeasibleInstanceException(expectedMessage);

        assertThat(exception.getMessage()).isEqualTo(expectedMessage);
    }
}
