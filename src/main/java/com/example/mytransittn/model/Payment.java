package com.example.mytransittn.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime transactionTime;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @ManyToOne
    private Journey journey;

    @ManyToOne(optional = false)
    private User user;

    private String transactionReference;

    public enum PaymentType { FARE_PAYMENT, BALANCE_TOPUP, REFUND }
    public enum PaymentStatus { PENDING, COMPLETED, FAILED, REFUNDED }


} 