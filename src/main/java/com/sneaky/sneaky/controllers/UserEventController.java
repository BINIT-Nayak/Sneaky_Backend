package com.sneaky.sneaky.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sneaky.sneaky.dto.analytics.UserEventRequest;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.analytics.UserEventTrackingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class UserEventController {
    private final CurrentUser currentUser;
    private final UserEventTrackingService userEventTrackingService;

    @PostMapping
    public ResponseEntity<Void> trackEvent(@Valid @RequestBody UserEventRequest request) {
        userEventTrackingService.track(currentUser.getUserId(), request);
        return ResponseEntity.accepted().build();
    }
}
