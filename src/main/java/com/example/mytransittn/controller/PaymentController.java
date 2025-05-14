package com.example.mytransittn.controller;

import com.example.mytransittn.dto.PaymentDto;
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
import java.util.stream.Collectors;

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
    public ResponseEntity<List<PaymentDto>> getUserPayments() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<PaymentDto> paymentDtos = paymentRepository.findByUserOrderByTransactionTimeDesc(user)
            .stream()
            .map(PaymentDto::fromEntity)
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(paymentDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDto> getPaymentById(@PathVariable Long id) {
        Optional<Payment> payment = paymentRepository.findById(id);
        
        if (payment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Check if the payment belongs to the current user or user is admin
        User currentUser = getCurrentUser();
        if (currentUser == null || (!payment.get().getUser().equals(currentUser) && !currentUser.isAdmin())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(PaymentDto.fromEntity(payment.get()));
    }

    @PostMapping("/topup")
    @Transactional
    public ResponseEntity<PaymentDto> topUpBalance(@RequestBody PaymentDto.TopupRequestDto request) {
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

        Payment savedPayment = paymentRepository.save(payment);
        return ResponseEntity.ok(PaymentDto.fromEntity(savedPayment));
    }

    @PostMapping("/journey/{journeyId}")
    @Transactional
    public ResponseEntity<PaymentDto> payForJourney(@PathVariable Long journeyId) {
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
            return ResponseEntity.badRequest().build();
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

        Payment savedPayment = paymentRepository.save(payment);
        return ResponseEntity.ok(PaymentDto.fromEntity(savedPayment));
    }

    @PostMapping("/refund/{paymentId}")
    @Transactional
    public ResponseEntity<PaymentDto> refundPayment(@PathVariable Long paymentId) {
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

        Payment savedRefund = paymentRepository.save(refund);
        return ResponseEntity.ok(PaymentDto.fromEntity(savedRefund));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaymentDto>> getPaymentsByStatus(@PathVariable String status) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.valueOf(status.toUpperCase());
            List<PaymentDto> paymentDtos = paymentRepository.findByUserAndStatus(user, paymentStatus)
                .stream()
                .map(PaymentDto::fromEntity)
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(paymentDtos);
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
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
} 