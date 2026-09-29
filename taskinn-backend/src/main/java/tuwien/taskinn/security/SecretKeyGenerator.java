package tuwien.taskinn.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * Utility class to generate a secure AES secret key encoded in Base64 format. This class demonstrates how to generate a
 * 256-bit AES key using Java's KeyGenerator for symmetric encryption purposes. Key points:
 * <ul>
 * <li>Uses AES algorithm with a 256-bit key size, which is considered very secure.</li>
 * <li>The generated SecretKey is encoded to Base64 to easily store or transmit the key.</li>
 * <li>This approach relies on the default secure random number generator of the JVM.</li>
 * <li>Make sure your Java runtime supports AES 256-bit keys (might require JCE Unlimited Strength Jurisdiction Policy
 * in older versions).</li>
 * </ul>
 * Usage: Run the main method to print a Base64 encoded AES key to the console.
 */
public class SecretKeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey secretKey = keyGen.generateKey();
        String base64Key = Base64.getEncoder().encodeToString(secretKey.getEncoded());
        System.out.println("Base64 AES key:");
        System.out.println(base64Key);
    }
}
