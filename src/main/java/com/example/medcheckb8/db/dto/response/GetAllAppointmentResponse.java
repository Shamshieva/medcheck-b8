package com.example.medcheckb8.db.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record GetAllAppointmentResponse(
        Long appointmentId,
        Long patientId,
        String fullName,
        String phoneNumber,
        String email,
        String department,
        String specialist,
        LocalDateTime localDateTime,
        boolean status
) {
}
