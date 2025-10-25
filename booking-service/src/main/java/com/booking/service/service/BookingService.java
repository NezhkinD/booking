package com.booking.service.service;

import com.booking.service.client.HotelClient;
import com.booking.service.dto.*;
import com.booking.service.entity.Booking;
import com.booking.service.entity.User;
import com.booking.service.exception.BookingException;
import com.booking.service.exception.ResourceNotFoundException;
import com.booking.service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final HotelClient hotelClient;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String username) {
        log.info("Creating booking for user: {}, correlationId: {}", username, request);

        validateDates(request.getStartDate(), request.getEndDate());

        User user = userService.getUserByUsername(username);

        String requestId = UUID.randomUUID().toString();

        if (bookingRepository.existsByRequestId(requestId)) {
            log.warn("Duplicate booking request detected: {}", requestId);
            return bookingRepository.findByRequestId(requestId)
                    .map(this::mapToResponse)
                    .orElseThrow(() -> new BookingException("Booking request already processed"));
        }

        Long roomId = request.getRoomId();

        if (request.isAutoSelect()) {
            roomId = selectRecommendedRoom(request);
        }

        if (roomId == null) {
            throw new BookingException("Room ID is required when autoSelect is false");
        }

        Booking booking = Booking.builder()
                .user(user)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Booking.BookingStatus.PENDING)
                .requestId(requestId)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking created with PENDING status: bookingId={}, requestId={}",
                booking.getId(), requestId);

        try {
            ConfirmAvailabilityRequest confirmRequest = ConfirmAvailabilityRequest.builder()
                    .requestId(requestId)
                    .bookingId(booking.getId())
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .build();

            log.debug("Confirming availability for room: {}, bookingId: {}", roomId, booking.getId());
            ConfirmAvailabilityResponse confirmResponse = hotelClient.confirmAvailability(roomId, confirmRequest);

            if (confirmResponse.isAvailable()) {
                booking.setStatus(Booking.BookingStatus.CONFIRMED);
                booking = bookingRepository.save(booking);
                log.info("Booking CONFIRMED: bookingId={}, reservationId={}",
                        booking.getId(), confirmResponse.getReservationId());
            } else {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                log.warn("Booking CANCELLED - room not available: bookingId={}", booking.getId());
                throw new BookingException("Room is not available for the selected dates");
            }

        } catch (Exception e) {
            log.error("Error during booking confirmation: bookingId={}, error={}",
                    booking.getId(), e.getMessage());

            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            try {
                ReleaseReservationRequest releaseRequest = ReleaseReservationRequest.builder()
                        .requestId(requestId)
                        .bookingId(booking.getId())
                        .build();

                hotelClient.releaseReservation(roomId, releaseRequest);
                log.info("Reservation released successfully for bookingId: {}", booking.getId());
            } catch (Exception releaseError) {
                log.error("Failed to release reservation: bookingId={}, error={}",
                        booking.getId(), releaseError.getMessage());
            }

            throw new BookingException("Failed to create booking: " + e.getMessage(), e);
        }

        return mapToResponse(booking);
    }

    private Long selectRecommendedRoom(BookingRequest request) {
        log.debug("Auto-selecting room for hotel: {}, dates: {} to {}",
                request.getHotelId(), request.getStartDate(), request.getEndDate());

        if (request.getHotelId() == null) {
            throw new BookingException("Hotel ID is required for auto-select");
        }

        List<RoomDTO> recommendedRooms = hotelClient.getRecommendedRooms(
                request.getHotelId(),
                request.getStartDate(),
                request.getEndDate()
        );

        if (recommendedRooms.isEmpty()) {
            throw new BookingException("No available rooms found for the selected dates");
        }

        Long selectedRoomId = recommendedRooms.get(0).getId();
        log.info("Auto-selected room: {} (timesBooked: {})",
                selectedRoomId, recommendedRooms.get(0).getTimesBooked());

        return selectedRoomId;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BookingException("Start date and end date are required");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new BookingException("Start date cannot be in the past");
        }

        if (endDate.isBefore(startDate)) {
            throw new BookingException("End date must be after start date");
        }
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String username) {
        log.debug("Fetching bookings for user: {}", username);

        User user = userService.getUserByUsername(username);

        return bookingRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<BookingResponse> getUserBookingsPaginated(
            String username, int page, int size) {
        log.debug("Fetching paginated bookings for user: {} (page={}, size={})", username, page, size);

        User user = userService.getUserByUsername(username);

        org.springframework.data.domain.Pageable pageable =
            org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        return bookingRepository.findByUser(user, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, String username) {
        log.debug("Fetching booking: {} for user: {}", bookingId, username);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new BookingException("Access denied to booking: " + bookingId);
        }

        return mapToResponse(booking);
    }

    @Transactional
    public void cancelBooking(Long bookingId, String username) {
        log.info("Cancelling booking: {} by user: {}", bookingId, username);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (!booking.getUser().getUsername().equals(username)) {
            throw new BookingException("Access denied to booking: " + bookingId);
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        try {
            ReleaseReservationRequest releaseRequest = ReleaseReservationRequest.builder()
                    .requestId(booking.getRequestId())
                    .bookingId(booking.getId())
                    .build();

            hotelClient.releaseReservation(booking.getRoomId(), releaseRequest);
            log.info("Reservation released for cancelled booking: {}", bookingId);
        } catch (Exception e) {
            log.error("Failed to release reservation for booking: {}, error: {}",
                    bookingId, e.getMessage());
        }
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .username(booking.getUser().getUsername())
                .roomId(booking.getRoomId())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
