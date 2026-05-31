package com.simulator.razorpay.repository;

import com.simulator.razorpay.entity.SimOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<SimOrder, String> {

    Optional<SimOrder> findByReceipt(String receipt);

    Page<SimOrder> findByStatus(SimOrder.OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM SimOrder o ORDER BY o.createdAt DESC")
    Page<SimOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(o) FROM SimOrder o WHERE o.status = ?1")
    Long countByStatus(SimOrder.OrderStatus status);

    @Query("SELECT SUM(o.amountPaid) FROM SimOrder o WHERE o.status = 'PAID'")
    Long getTotalRevenue();

    List<SimOrder> findByCreatedAtBetween(Long startTime, Long endTime);
}
