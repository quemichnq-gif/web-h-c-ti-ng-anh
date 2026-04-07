package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.List;
import java.util.Optional;

@ControllerAdvice
public class GlobalNotificationAdvice {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public GlobalNotificationAdvice(UserRepository userRepository,
                                    NotificationService notificationService) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @ModelAttribute("notificationsPreview")
    public List<NotificationService.NotificationItem> notificationsPreview(Authentication authentication) {
        return notificationService.buildNotifications(resolveCurrentUser(authentication).orElse(null));
    }

    @ModelAttribute("notificationCount")
    public int notificationCount(Authentication authentication) {
        return notificationsPreview(authentication).size();
    }

    private Optional<User> resolveCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()));
    }
}
