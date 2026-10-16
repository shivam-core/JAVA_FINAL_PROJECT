package com.shivamcore.loanapp.util;

import com.shivamcore.loanapp.dao.UserDao;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates user credentials for guest flow.
 * Username format: First 3 letters of fname (Capitalised first, rest lowercase)
 * + First 3 letters of lname (Capitalised first, rest lowercase)
 * + Last 4 digits of mobile.
 * Collision resolution appends a 2-digit random suffix (Decision 12).
 * Password: 8-10 chars, SecureRandom, meets password policy.
 */
public final class CredentialGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "@#$%^&+=!";
    private static final String ALL_CHARS = UPPER + LOWER + DIGITS + SYMBOLS;

    private CredentialGenerator() {
        // Utility class
    }

    public static String baseUsername(String fname, String lname, String mobile) {
        String cleanFname = fname == null ? "" : fname.trim();
        String cleanLname = lname == null ? "" : lname.trim();
        String cleanMobile = mobile == null ? "" : mobile.trim();

        String fPart = formatNamePart(cleanFname, 3);
        String lPart = formatNamePart(cleanLname, 3);
        String mPart = cleanMobile.length() >= 4
                ? cleanMobile.substring(cleanMobile.length() - 4)
                : cleanMobile;

        return fPart + lPart + mPart;
    }

    private static String formatNamePart(String name, int maxLen) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        int len = Math.min(name.length(), maxLen);
        String sub = name.substring(0, len);
        if (sub.length() == 1) {
            return sub.toUpperCase();
        }
        return sub.substring(0, 1).toUpperCase() + sub.substring(1).toLowerCase();
    }

    public static String generateUsername(String fname, String lname, String mobile, UserDao userDao) {
        String base = baseUsername(fname, lname, mobile);
        if (userDao == null || !userDao.existsByUsername(base)) {
            return base;
        }

        // Decision 12: append random 2-digit suffix and retry up to 5 times
        for (int i = 0; i < 5; i++) {
            int suffix = 10 + RANDOM.nextInt(90);
            String candidate = base + suffix;
            if (!userDao.existsByUsername(candidate)) {
                return candidate;
            }
        }
        return base + (1000 + RANDOM.nextInt(9000));
    }

    public static String generatePassword() {
        int length = 10;
        List<Character> chars = new ArrayList<>();
        chars.add(UPPER.charAt(RANDOM.nextInt(UPPER.length())));
        chars.add(LOWER.charAt(RANDOM.nextInt(LOWER.length())));
        chars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        chars.add(SYMBOLS.charAt(RANDOM.nextInt(SYMBOLS.length())));

        for (int i = 4; i < length; i++) {
            chars.add(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }

        Collections.shuffle(chars, RANDOM);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        String pwd = sb.toString();
        if (!Validator.isValidPassword(pwd)) {
            return generatePassword();
        }
        return pwd;
    }
}
