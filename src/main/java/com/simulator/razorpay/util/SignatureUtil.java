package com.simulator.razorpay.util;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Utility for generating and verifying HMAC signatures.
 * Uses the same algorithm as Razorpay for webhook signature verification.
 */
public final class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private SignatureUtil() {
        // Utility class
    }

    /**
     * Generates HMAC-SHA256 signature for the given payload.
     * This is used for webhook signatures.
     *
     * @param payload The payload to sign
     * @param secret  The webhook secret
     * @return Hex-encoded signature
     */
    public static String generateSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Verifies a webhook signature.
     *
     * @param payload           The webhook payload
     * @param receivedSignature The signature from the webhook header
     * @param secret            The webhook secret
     * @return true if signature is valid
     */
    public static boolean verifySignature(String payload, String receivedSignature, String secret) {
        String expectedSignature = generateSignature(payload, secret);
        return constantTimeEquals(expectedSignature, receivedSignature);
    }

    /**
     * Generates signature for payment verification.
     * Used to verify payment after checkout: order_id|razorpay_payment_id
     *
     * @param orderId   The order ID
     * @param paymentId The payment ID
     * @param secret    The API key secret
     * @return Hex-encoded signature
     */
    public static String generatePaymentSignature(String orderId, String paymentId, String secret) {
        String payload = orderId + "|" + paymentId;
        return generateSignature(payload, secret);
    }

    /**
     * Verifies payment signature after checkout.
     *
     * @param orderId           The order ID
     * @param paymentId         The payment ID
     * @param receivedSignature The signature from checkout response
     * @param secret            The API key secret
     * @return true if signature is valid
     */
    public static boolean verifyPaymentSignature(
            String orderId,
            String paymentId,
            String receivedSignature,
            String secret) {
        String expectedSignature = generatePaymentSignature(orderId, paymentId, secret);
        return constantTimeEquals(expectedSignature, receivedSignature);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
