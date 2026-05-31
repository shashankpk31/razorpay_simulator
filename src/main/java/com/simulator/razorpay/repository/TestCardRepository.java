package com.simulator.razorpay.repository;

import com.simulator.razorpay.entity.TestCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestCardRepository extends JpaRepository<TestCard, String> {

    Optional<TestCard> findByCardNumber(String cardNumber);

    List<TestCard> findBySimulateResult(TestCard.SimulateResult result);

    List<TestCard> findByCardNetwork(String network);

    List<TestCard> findByCardType(String type);
}
