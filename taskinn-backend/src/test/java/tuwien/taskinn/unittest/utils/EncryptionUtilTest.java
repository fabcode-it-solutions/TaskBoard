package tuwien.taskinn.unittest.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tuwien.taskinn.config.EncryptionProperties;
import tuwien.taskinn.util.EncryptionUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("EncryptionUtil Unit Tests")
public class EncryptionUtilTest {

    @Mock
    private EncryptionProperties encryptionProperties;

    @InjectMocks
    private EncryptionUtil encryptionUtil;

    @Test
    @DisplayName("Encrypt and decrypt returns original content")
    void givenByteArray_whenEncryptedThenDecrypted_thenOriginalReturned() {
        when(encryptionProperties.getKey()).thenReturn("GWz44lWq9VF/WQtUPtl4NKIduo6c87gTeX3spivrU0Y=");

        encryptionUtil.init();

        byte[] original = "test-data".getBytes();
        byte[] encrypted = encryptionUtil.encrypt(original);
        byte[] decrypted = encryptionUtil.decrypt(encrypted);

        assertAll(() -> assertNotNull(encrypted), () -> assertNotEquals(new String(encrypted), new String(original)),
                () -> assertArrayEquals(decrypted, original));
    }
}
