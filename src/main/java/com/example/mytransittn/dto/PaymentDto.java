package com.example.mytransittn.dto;

import com.example.mytransittn.model.Payment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDto {
    private Long id;
    private BigDecimal amount;
    private LocalDateTime transactionTime;
    private Payment.PaymentType paymentType;
    private Payment.PaymentStatus status;
    private Long journeyId;
    private String journeyDescription;
    private UserSummaryDto user;
    private String transactionReference;

    public static PaymentDto fromEntity(Payment payment) {
        if (payment == null) {
            return null;
        }
        
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId());
        dto.setAmount(payment.getAmount());
        dto.setTransactionTime(payment.getTransactionTime());
        dto.setPaymentType(payment.getPaymentType());
        dto.setStatus(payment.getStatus());
        dto.setTransactionReference(payment.getTransactionReference());
        
        if (payment.getJourney() != null) {
            dto.setJourneyId(payment.getJourney().getId());
            // Create a simple description for the journey
            String journeyDesc = payment.getJourney().getStartStation().getName() + 
                                " to " + 
                                payment.getJourney().getEndStation().getName();
            dto.setJourneyDescription(journeyDesc);
        }
        
        if (payment.getUser() != null) {
            UserSummaryDto userDto = new UserSummaryDto();
            userDto.setId(payment.getUser().getId());
            userDto.setUsername(payment.getUser().getUsername());
            userDto.setEmail(payment.getUser().getEmail());
            dto.setUser(userDto);
        }
        
        return dto;
    }
    
    @Data
    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String email;
    }
    
    @Data
    public static class TopupRequestDto {
        private BigDecimal amount;
    }
} 