package com.example.mytransittn.controller;

import com.example.mytransittn.model.Journey;
import com.example.mytransittn.model.Payment;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.JourneyRepository;
import com.example.mytransittn.repository.PaymentRepository;
import com.example.mytransittn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final JourneyRepository journeyRepository;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository, UserRepository userRepository,
                           JourneyRepository journeyRepository) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.journeyRepository = journeyRepository;
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getUserPayments() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(paymentRepository.findByUserOrderByTransactionTimeDesc(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        Optional<Payment> payment = paymentRepository.findById(id);
        
        if (payment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the payment belongs to the current user or user is admin
        User currentUser = getCurrentUser();
        if (currentUser == null || (!payment.get().getUser().equals(currentUser) && !currentUser.isAdmin())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(payment.get());
    }

    @PostMapping("/topup")
    @Transactional
    public ResponseEntity<Payment> topUpBalance(@RequestBody TopupRequest request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }

        // Create payment
        Payment payment = new Payment();
        payment.setAmount(request.getAmount());
        payment.setTransactionTime(LocalDateTime.now());
        payment.setPaymentType(Payment.PaymentType.BALANCE_TOPUP);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setUser(user);
        payment.setTransactionReference(generateTransactionReference());

        // Update user balance
        user.setBalance(user.getBalance().add(request.getAmount()));
        userRepository.save(user);

        return ResponseEntity.ok(paymentRepository.save(payment));
    }

    @PostMapping("/journey/{journeyId}")
    @Transactional
    public ResponseEntity<Payment> payForJourney(@PathVariable Long journeyId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Journey> journeyOpt = journeyRepository.findById(journeyId);
        if (journeyOpt.isEmpty() || !journeyOpt.get().getUser().equals(user)) {
            return ResponseEntity.badRequest().build();
        }

        Journey journey = journeyOpt.get();
        
        // Check if journey is completed and has a fare
        if (journey.getStatus() != Journey.JourneyStatus.COMPLETED || journey.getFare() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Check if already paid
        List<Payment> existingPayments = paymentRepository.findByJourneyId(journeyId);
        if (!existingPayments.isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        // Check balance
        if (user.getBalance().compareTo(journey.getFare()) < 0) {
            return ResponseEntity.badRequest().build();
        }

        // Create payment
        Payment payment = new Payment();
        payment.setAmount(journey.getFare());
        payment.setTransactionTime(LocalDateTime.now());
        payment.setPaymentType(Payment.PaymentType.FARE_PAYMENT);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setUser(user);
        payment.setJourney(journey);
        payment.setTransactionReference(generateTransactionReference());

        // Update user balance
        user.setBalance(user.getBalance().subtract(journey.getFare()));
        userRepository.save(user);

        return ResponseEntity.ok(paymentRepository.save(payment));
    }

    @PostMapping("/refund/{paymentId}")
    @Transactional
    public ResponseEntity<Payment> refundPayment(@PathVariable Long paymentId) {
        User user = getCurrentUser();
        if (user == null || !user.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Payment originalPayment = paymentOpt.get();
        
        // Can only refund completed fare payments
        if (originalPayment.getStatus() != Payment.PaymentStatus.COMPLETED || 
            originalPayment.getPaymentType() != Payment.PaymentType.FARE_PAYMENT) {
            return ResponseEntity.badRequest().build();
        }

        // Mark original payment as refunded
        originalPayment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(originalPayment);

        // Create refund payment
        Payment refund = new Payment();
        refund.setAmount(originalPayment.getAmount());
        refund.setTransactionTime(LocalDateTime.now());
        refund.setPaymentType(Payment.PaymentType.REFUND);
        refund.setStatus(Payment.PaymentStatus.COMPLETED);
        refund.setUser(originalPayment.getUser());
        refund.setJourney(originalPayment.getJourney());
        refund.setTransactionReference(generateTransactionReference());

        // Update user balance
        User refundUser = originalPayment.getUser();
        refundUser.setBalance(refundUser.getBalance().add(originalPayment.getAmount()));
        userRepository.save(refundUser);

        return ResponseEntity.ok(paymentRepository.save(refund));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Payment>> getPaymentsByStatus(@PathVariable String status) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            return ResponseEntity.ok(paymentRepository.findByUserAndStatus(user, paymentStatus));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    private String generateTransactionReference() {
        return UUID.randomUUID().toString();
    }

    // Request DTO for balance top-up
    static class TopupRequest {
        private BigDecimal amount;

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }
} 