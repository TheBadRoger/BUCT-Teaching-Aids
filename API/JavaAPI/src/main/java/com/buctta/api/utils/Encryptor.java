package com.buctta.api.utils;

public class Encryptor {
    public static String CaeserEncrypt(String input, int shift) {
        StringBuilder result = new StringBuilder();
        for (char character : input.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isLowerCase(character) ? 'a' : 'A';
                char encryptedChar = (char) ((character - base + shift) % 26 + base);
                result.append(encryptedChar);
            }
            else
                result.append(character);
        }
        return result.toString();
    }

    public static String CaeserDecrypt(String input, int shift) {
        return CaeserEncrypt(input, 26 - (shift % 26));
    }

    public static String XOREncrypt(String input, char key) {
        StringBuilder result = new StringBuilder();
        for (char character : input.toCharArray()) {
            char encryptedChar = (char) (character ^ key);
            result.append(encryptedChar);
        }
        return result.toString();
    }

    public static String XORDecrypt(String input, char key) {
        return XOREncrypt(input, key);
    }

    public static String Base64Encode(String input) {
        return java.util.Base64.getEncoder().encodeToString(input.getBytes());
    }

    public static String Base64Decode(String input) {
        return java.util.Base64.getEncoder().encodeToString(input.getBytes());
    }

    public static String MD5Hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String SHA256Hash(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
