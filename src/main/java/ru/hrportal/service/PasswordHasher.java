package ru.hrportal.service;

import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {
  public byte[] salt() {
    byte[] bytes = new byte[16];
    new SecureRandom().nextBytes(bytes);
    return bytes;
  }

  public byte[] hash(String password, byte[] salt) {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120000, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      spec.clearPassword();
    }
  }

  public boolean matches(String password, byte[] salt, byte[] expected) {
    return MessageDigest.isEqual(hash(password, salt), expected);
  }
}
