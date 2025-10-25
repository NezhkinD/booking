package com.booking.hotel.service;

import com.booking.hotel.dto.ConfirmAvailabilityRequest;
import com.booking.hotel.dto.ConfirmAvailabilityResponse;
import com.booking.hotel.dto.ReleaseReservationRequest;
import com.booking.hotel.entity.Room;
import com.booking.hotel.entity.RoomReservation;
import com.booking.hotel.exception.ResourceNotFoundException;
import com.booking.hotel.exception.RoomNotAvailableException;
import com.booking.hotel.repository.RoomRepository;
import com.booking.hotel.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomReservationService {

    private final RoomReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;

    @Transactional
    public ConfirmAvailabilityResponse confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        log.info("Confirming availability for room: {}, bookingId: {}, requestId: {}",
                roomId, request.getBookingId(), request.getRequestId());

        Optional<RoomReservation> existingReservation = reservationRepository.findByRequestId(request.getRequestId());
        if (existingReservation.isPresent()) {
            RoomReservation reservation = existingReservation.get();
            log.info("Duplicate request detected, returning existing reservation: id={}, status={}",
                    reservation.getId(), reservation.getStatus());

            boolean isAvailable = reservation.getStatus() != RoomReservation.ReservationStatus.RELEASED;
            return ConfirmAvailabilityResponse.builder()
                    .available(isAvailable)
                    .message(isAvailable ? "Reservation already exists" : "Reservation was released")
                    .reservationId(reservation.getId())
                    .build();
        }

        Room room = roomService.getRoomById(roomId);

        if (!room.getAvailable()) {
            log.warn("Room is not operationally available: roomId={}", roomId);
            throw new RoomNotAvailableException("Room is not available");
        }

        if (!isRoomAvailableForDates(roomId, request.getStartDate(), request.getEndDate())) {
            log.warn("Room is already booked for the requested dates: roomId={}, dates: {} to {}",
                    roomId, request.getStartDate(), request.getEndDate());

            createReleasedReservation(room, request);

            return ConfirmAvailabilityResponse.builder()
                    .available(false)
                    .message("Room is not available for the selected dates")
                    .reservationId(null)
                    .build();
        }

        RoomReservation reservation = RoomReservation.builder()
                .room(room)
                .bookingId(request.getBookingId())
                .requestId(request.getRequestId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(RoomReservation.ReservationStatus.CONFIRMED)
                .build();

        reservation = reservationRepository.save(reservation);

        room.incrementBookingCount();
        roomRepository.save(room);

        log.info("Reservation CONFIRMED: id={}, roomId={}, bookingId={}, timesBooked={}",
                reservation.getId(), roomId, request.getBookingId(), room.getTimesBooked());

        return ConfirmAvailabilityResponse.builder()
                .available(true)
                .message("Room reserved successfully")
                .reservationId(reservation.getId())
                .build();
    }

    @Transactional
    public void releaseReservation(Long roomId, ReleaseReservationRequest request) {
        log.info("Releasing reservation for room: {}, bookingId: {}, requestId: {}",
                roomId, request.getBookingId(), request.getRequestId());

        Optional<RoomReservation> reservationOpt = reservationRepository.findByBookingId(request.getBookingId());

        if (reservationOpt.isEmpty()) {
            log.warn("Reservation not found for bookingId: {}", request.getBookingId());
            return;
        }

        RoomReservation reservation = reservationOpt.get();

        if (reservation.getStatus() == RoomReservation.ReservationStatus.RELEASED) {
            log.info("Reservation already released: id={}", reservation.getId());
            return;
        }

        reservation.setStatus(RoomReservation.ReservationStatus.RELEASED);
        reservationRepository.save(reservation);

        log.info("Reservation RELEASED: id={}, roomId={}, bookingId={}",
                reservation.getId(), roomId, request.getBookingId());
    }

    private boolean isRoomAvailableForDates(Long roomId, LocalDate startDate, LocalDate endDate) {
        return roomRepository.findAvailableRoomsForDates(startDate, endDate)
                .stream()
                .anyMatch(room -> room.getId().equals(roomId));
    }

    private void createReleasedReservation(Room room, ConfirmAvailabilityRequest request) {
        RoomReservation reservation = RoomReservation.builder()
                .room(room)
                .bookingId(request.getBookingId())
                .requestId(request.getRequestId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(RoomReservation.ReservationStatus.RELEASED)
                .build();

        reservationRepository.save(reservation);
        log.debug("Created RELEASED reservation for idempotency: requestId={}", request.getRequestId());
    }
}
