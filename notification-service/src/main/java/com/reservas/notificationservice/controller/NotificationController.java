package com.reservas.notificationservice.controller;

import com.reservas.common.security.AuthenticatedUser;
import com.reservas.notificationservice.dto.NotificationResponse;
import com.reservas.notificationservice.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    public Page<NotificationResponse> myNotifications(
            Authentication authentication,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        return notificationService.listForUser(user.id(), pageable);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<NotificationResponse> allNotifications(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return notificationService.listAll(pageable);
    }
}
