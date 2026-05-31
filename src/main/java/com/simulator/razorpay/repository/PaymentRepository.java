package com.simulator.razorpay.repository;

import com.simulator.razorpay.entity.SimPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<SimPayment, String> {

    List<SimPayment> findByOrderId(String orderId);

    Page<SimPayment> findByStatus(SimPayment.PaymentStatus status, Pageable pageable);

    @Query("SELECT p FROM SimPayment p ORDER BY p.createdAt DESC")
    Page<SimPayment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(p) FROM SimPayment p WHERE p.status = ?1")
    Long countByStatus(SimPayment.PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM SimPayment p WHERE p.status = 'CAPTURED'")
    Long getTotalCapturedAmount();

    List<SimPayment> findByEmail(String email);

    List<SimPayment> findByContact(String contact);

    @Query("SELECT p.method, COUNT(p) FROM SimPayment p WHERE p.status = 'CAPTURED' GROUP BY p.method")
    List<Object[]> getPaymentMethodStats();
}
