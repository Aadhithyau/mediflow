package com.mediflow.payment;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface PaymentRepository
    extends JpaRepository<Payment, Long> {

    Optional<Payment> findByAppointment_Id(
        Long appointmentId
    );

    boolean existsByRazorpayWebhookEventId(
        String razorpayWebhookEventId
    );

    @Query("""
        SELECT payment
        FROM Payment payment
        JOIN FETCH payment.appointment appointment
        WHERE appointment.id = :appointmentId
          AND appointment.patient.id = :patientUserId
        """)
    Optional<Payment> findForPatient(
        @Param("appointmentId") Long appointmentId,
        @Param("patientUserId") Long patientUserId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT payment
        FROM Payment payment
        WHERE payment.razorpayPaymentLinkId = :paymentLinkId
        """)
    Optional<Payment> findByPaymentLinkIdForUpdate(
        @Param("paymentLinkId") String paymentLinkId
    );
}