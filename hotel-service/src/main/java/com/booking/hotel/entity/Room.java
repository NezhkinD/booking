package com.booking.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms",
    indexes = {
        @Index(name = "idx_hotel_id", columnList = "hotel_id"),
        @Index(name = "idx_available", columnList = "available")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_hotel_room_number", columnNames = {"hotel_id", "number"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 20)
    private String number;

    @Column(nullable = false)
    @Builder.Default
    private Boolean available = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer timesBooked = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void incrementBookingCount() {
        this.timesBooked++;
    }
}
