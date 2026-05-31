package com.simulator.razorpay.service;

import com.simulator.razorpay.entity.TestCard;
import com.simulator.razorpay.repository.TestCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initializes test data on application startup.
 * Creates test cards matching Razorpay's test card numbers.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final TestCardRepository testCardRepository;

    @Override
    public void run(String... args) {
        if (testCardRepository.count() > 0) {
            log.info("Test cards already initialized");
            return;
        }

        log.info("Initializing test cards...");

        List<TestCard> testCards = List.of(
                // ========== SUCCESS CARDS ==========
                TestCard.builder()
                        .cardNumber("4111111111111111")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.SUCCESS)
                        .description("Visa Credit - Always succeeds")
                        .build(),

                TestCard.builder()
                        .cardNumber("5267318187975449")
                        .cardNetwork("Mastercard")
                        .cardType("debit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.SUCCESS)
                        .description("Mastercard Debit - Always succeeds")
                        .build(),

                TestCard.builder()
                        .cardNumber("4012888888881881")
                        .cardNetwork("Visa")
                        .cardType("debit")
                        .issuer("HDFC Bank")
                        .simulateResult(TestCard.SimulateResult.SUCCESS)
                        .description("Visa Debit - HDFC")
                        .build(),

                TestCard.builder()
                        .cardNumber("5105105105105100")
                        .cardNetwork("Mastercard")
                        .cardType("credit")
                        .issuer("ICICI Bank")
                        .simulateResult(TestCard.SimulateResult.SUCCESS)
                        .description("Mastercard Credit - ICICI")
                        .build(),

                TestCard.builder()
                        .cardNumber("6011111111111117")
                        .cardNetwork("RuPay")
                        .cardType("debit")
                        .issuer("SBI")
                        .simulateResult(TestCard.SimulateResult.SUCCESS)
                        .description("RuPay Debit - SBI")
                        .build(),

                TestCard.builder()
                        .cardNumber("378282246310005")
                        .cardNetwork("Amex")
                        .cardType("credit")
                        .issuer("American Express")
                        .simulateResult(TestCard.SimulateResult.SUCCESS)
                        .description("Amex - Always succeeds")
                        .build(),

                // ========== FAILURE CARDS ==========
                TestCard.builder()
                        .cardNumber("4000000000000002")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.CARD_DECLINED)
                        .failureCode("BAD_REQUEST_ERROR")
                        .failureDescription("Card declined by issuing bank")
                        .description("Card Declined - Bank rejected")
                        .build(),

                TestCard.builder()
                        .cardNumber("4000000000009995")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.INSUFFICIENT_FUNDS)
                        .failureCode("BAD_REQUEST_ERROR")
                        .failureDescription("Insufficient funds in account")
                        .description("Insufficient Funds")
                        .build(),

                TestCard.builder()
                        .cardNumber("4000000000000069")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.EXPIRED_CARD)
                        .failureCode("BAD_REQUEST_ERROR")
                        .failureDescription("Card has expired")
                        .description("Expired Card")
                        .build(),

                TestCard.builder()
                        .cardNumber("4000000000000127")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.INVALID_CVV)
                        .failureCode("BAD_REQUEST_ERROR")
                        .failureDescription("Invalid CVV")
                        .description("Invalid CVV")
                        .build(),

                // ========== 3DS / OTP CARDS ==========
                TestCard.builder()
                        .cardNumber("4000000000003220")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.OTP_REQUIRED)
                        .description("3DS Required - Will prompt for OTP")
                        .processingDelayMs(2000)
                        .build(),

                // ========== TIMEOUT CARD ==========
                TestCard.builder()
                        .cardNumber("4000000000000044")
                        .cardNetwork("Visa")
                        .cardType("credit")
                        .issuer("Test Bank")
                        .simulateResult(TestCard.SimulateResult.TIMEOUT)
                        .failureCode("GATEWAY_ERROR")
                        .failureDescription("Payment gateway timeout")
                        .description("Timeout - Simulates slow response")
                        .processingDelayMs(30000)
                        .build()
        );

        testCardRepository.saveAll(testCards);
        log.info("Initialized {} test cards", testCards.size());
    }
}
