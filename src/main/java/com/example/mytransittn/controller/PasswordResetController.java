package com.example.mytransittn.controller;


import com.example.mytransittn.model.PasswordResetToken;
import com.example.mytransittn.model.User;
import com.example.mytransittn.repository.PasswordResetTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/password-reset")
public class PasswordResetController {
    private final PasswordResetTokenRepository tokenRepository;
    public PasswordResetController(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }
    @GetMapping
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        Optional<PasswordResetToken> optionalToken = tokenRepository.findByToken(token);
        if (optionalToken.isEmpty() || optionalToken.get().isExpired()) {
            model.addAttribute("error", "Jeton invalide ou expiré");
            return "reset-password-error";
        }
        model.addAttribute("token", token);
        return "reset-password-form";
    }

    @PostMapping
    public String handlePasswordReset(
            @RequestParam String token,
            @RequestParam String newPassword,
            Model model) {
        Optional<PasswordResetToken> optionalToken = tokenRepository.findByToken(token);
        if (optionalToken.isEmpty() || optionalToken.get().isExpired()) {
            model.addAttribute("error", "Jeton invalide ou expiré");
            return "reset-password-error";
        }
        PasswordResetToken resetToken = optionalToken.get();
        User user = resetToken.getUser();
        tokenRepository.delete(resetToken);
        return "redirect:/login?resetSuccess";
    }
}