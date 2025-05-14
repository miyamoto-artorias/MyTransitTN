package com.example.mytransittn.repository;

import com.example.mytransittn.model.Payment;
import com.example.mytransittn.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUser(User user);
    
    List<Payment> findByUserOrderByTransactionTimeDesc(User user);
    
    @Query("SELECT p FROM Payment p WHERE p.user = :user AND p.status = :status")
    List<Payment> findByUserAndStatus(@Param("user") User user, @Param("status") Payment.PaymentStatus status);
    
    @Query("SELECT p FROM Payment p WHERE p.transactionTime BETWEEN :start AND :end")
    List<Payment> findPaymentsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    
    @Query("SELECT p FROM Payment p WHERE p.journey.id = :journeyId")
    List<Payment> findByJourneyId(@Param("journeyId") Long journeyId);
    
    @Query("SELECT p FROM Payment p WHERE p.paymentType = :type")
    List<Payment> findByPaymentType(@Param("type") Payment.PaymentType type);
} 