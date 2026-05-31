package com.simulator.razorpay.repository;

import com.simulator.razorpay.entity.SimRefund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<SimRefund, String> {

    List<SimRefund> findByPaymentId(String paymentId);

    Page<SimRefund> findByStatus(SimRefund.RefundStatus status, Pageable pageable);

    @Query("SELECT r FROM SimRefund r ORDER BY r.createdAt DESC")
    Page<SimRefund> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT SUM(r.amount) FROM SimRefund r WHERE r.status = 'PROCESSED'")
    Long getTotalRefundedAmount();

    List<SimRefund> findByStatusAndCreatedAtBefore(SimRefund.RefundStatus status, Long timestamp);
}
