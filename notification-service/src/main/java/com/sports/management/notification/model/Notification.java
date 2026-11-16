package com.sports.management.notification.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Recipient is required")
    @Column(nullable = false)
    private String recipient;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Message is required")
    @Column(nullable = false, length = 1000)
    private String message;

    private String type; // MATCH_FINISHED, TOURNAMENT_CREATED, etc.

    private Long referenceId;

    private LocalDateTime createdAt;

    private Boolean isRead = false;

    public Notification() {
    }

    public Notification(Long id, String recipient, String title, String message, String type, Long referenceId, LocalDateTime createdAt, Boolean isRead) {
        this.id = id;
        this.recipient = recipient;
        this.title = title;
        this.message = message;
        this.type = type;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
        this.isRead = isRead != null ? isRead : false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getRead() {
        return isRead != null ? isRead : false;
    }

    public Boolean isRead() {
        return isRead != null ? isRead : false;
    }

    public void setRead(Boolean read) {
        isRead = read != null ? read : false;
    }
}
