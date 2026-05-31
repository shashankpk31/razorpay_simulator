package com.simulator.razorpay.util;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Generates Razorpay-style IDs for various entities.
 * IDs follow the format: {prefix}_{random_string}
 */
public final class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private IdGenerator() {
        // Utility class
    }

    /**
     * Generates a random string of specified length.
     */
    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * Generates an order ID: order_xxxxxxxxxx
     */
    public static String orderId() {
        return "order_" + randomString(14);
    }

    /**
     * Generates a payment ID: pay_xxxxxxxxxx
     */
    public static String paymentId() {
        return "pay_" + randomString(14);
    }

    /**
     * Generates a refund ID: rfnd_xxxxxxxxxx
     */
    public static String refundId() {
        return "rfnd_" + randomString(14);
    }

    /**
     * Generates a card ID: card_xxxxxxxxxx
     */
    public static String cardId() {
        return "card_" + randomString(14);
    }

    /**
     * Generates an event ID: evt_xxxxxxxxxx
     */
    public static String eventId() {
        return "evt_" + randomString(14);
    }

    /**
     * Generates a bank transaction ID (UTR format).
     */
    public static String bankTransactionId() {
        return "UTR" + System.currentTimeMillis() + randomString(6).toUpperCase();
    }

    /**
     * Generates a 6-digit OTP.
     */
    public static String otp() {
        return String.format("%06d", RANDOM.nextInt(1000000));
    }

    /**
     * Generates a 6-character auth code.
     */
    public static String authCode() {
        return randomString(6).toUpperCase();
    }
}
