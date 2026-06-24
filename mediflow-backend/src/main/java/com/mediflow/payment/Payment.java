package com.mediflow.payment;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.mediflow.appointment.Appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "appointment_id",
        nullable = false,
        unique = true
    )
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(
        name = "razorpay_reference_id",
        length = 40,
        unique = true
    )
    private String razorpayReferenceId;

    @Column(
        name = "razorpay_payment_link_id",
        length = 100,
        unique = true
    )
    private String razorpayPaymentLinkId;

    @Column(
        name = "razorpay_payment_link_url",
        length = 500
    )
    private String razorpayPaymentLinkUrl;

    @Column(
        name = "razorpay_payment_id",
        length = 100,
        unique = true
    )
    private String razorpayPaymentId;

    @Column(
        name = "razorpay_webhook_event_id",
        length = 100,
        unique = true
    )
    private String razorpayWebhookEventId;

    @Column(
        name = "failure_message",
        length = 500
    )
    private String failureMessage;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Payment() {
    }

    @PrePersist
    void beforeInsert() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRazorpayReferenceId() {
        return razorpayReferenceId;
    }

    public void setRazorpayReferenceId(
        String razorpayReferenceId
    ) {
        this.razorpayReferenceId = razorpayReferenceId;
    }

    public String getRazorpayPaymentLinkId() {
        return razorpayPaymentLinkId;
    }

    public void setRazorpayPaymentLinkId(
        String razorpayPaymentLinkId
    ) {
        this.razorpayPaymentLinkId =
            razorpayPaymentLinkId;
    }

    public String getRazorpayPaymentLinkUrl() {
        return razorpayPaymentLinkUrl;
    }

    public void setRazorpayPaymentLinkUrl(
        String razorpayPaymentLinkUrl
    ) {
        this.razorpayPaymentLinkUrl =
            razorpayPaymentLinkUrl;
    }

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(
        String razorpayPaymentId
    ) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayWebhookEventId() {
        return razorpayWebhookEventId;
    }

    public void setRazorpayWebhookEventId(
        String razorpayWebhookEventId
    ) {
        this.razorpayWebhookEventId =
            razorpayWebhookEventId;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(
        String failureMessage
    ) {
        this.failureMessage = failureMessage;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(OffsetDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}