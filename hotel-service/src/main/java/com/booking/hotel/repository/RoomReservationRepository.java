package com.booking.hotel.repository;

import com.booking.hotel.entity.RoomReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {

    Optional<RoomReservation> findByRequestId(String requestId);

    Optional<RoomReservation> findByBookingId(Long bookingId);

    boolean existsByRequestId(String requestId);

    long countByRoomIdAndStatus(Long roomId, RoomReservation.ReservationStatus status);
}
