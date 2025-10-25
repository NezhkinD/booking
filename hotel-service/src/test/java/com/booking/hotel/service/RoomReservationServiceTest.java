package com.booking.hotel.service;

import com.booking.hotel.dto.ConfirmAvailabilityRequest;
import com.booking.hotel.dto.ConfirmAvailabilityResponse;
import com.booking.hotel.dto.ReleaseReservationRequest;
import com.booking.hotel.entity.Hotel;
import com.booking.hotel.entity.Room;
import com.booking.hotel.entity.RoomReservation;
import com.booking.hotel.exception.RoomNotAvailableException;
import com.booking.hotel.repository.RoomRepository;
import com.booking.hotel.repository.RoomReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomReservationServiceTest {

    @Mock
    private RoomReservationRepository reservationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomReservationService reservationService;

    private Hotel testHotel;
    private Room testRoom;
    private ConfirmAvailabilityRequest confirmRequest;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("123 Test St")
                .build();

        testRoom = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .createdAt(LocalDateTime.now())
                .build();

        confirmRequest = ConfirmAvailabilityRequest.builder()
                .requestId("test-request-id")
                .bookingId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();
    }

    @Test
    void confirmAvailability_WithAvailableRoom_ShouldCreateConfirmedReservation() {
        // Arrange
        when(reservationRepository.findByRequestId("test-request-id")).thenReturn(Optional.empty());
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomRepository.findAvailableRoomsForDates(any(), any()))
                .thenReturn(Arrays.asList(testRoom));
        when(reservationRepository.save(any(RoomReservation.class))).thenAnswer(i -> {
            RoomReservation reservation = i.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });

        // Act
        ConfirmAvailabilityResponse response = reservationService.confirmAvailability(1L, confirmRequest);

        // Assert
        assertTrue(response.isAvailable());
        assertNotNull(response.getReservationId());
        assertEquals("Room reserved successfully", response.getMessage());
        verify(reservationRepository).save(any(RoomReservation.class));
        verify(roomRepository).save(testRoom);
        assertEquals(1, testRoom.getTimesBooked());
    }

    @Test
    void confirmAvailability_WithDuplicateRequest_ShouldReturnExistingReservation() {
        // Arrange
        RoomReservation existingReservation = RoomReservation.builder()
                .id(1L)
                .room(testRoom)
                .bookingId(1L)
                .requestId("test-request-id")
                .startDate(confirmRequest.getStartDate())
                .endDate(confirmRequest.getEndDate())
                .status(RoomReservation.ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findByRequestId("test-request-id"))
                .thenReturn(Optional.of(existingReservation));

        // Act
        ConfirmAvailabilityResponse response = reservationService.confirmAvailability(1L, confirmRequest);

        // Assert
        assertTrue(response.isAvailable());
        assertEquals(1L, response.getReservationId());
        verify(reservationRepository, never()).save(any());
        verify(roomRepository, never()).save(any());
    }

    @Test
    void confirmAvailability_WithUnavailableRoom_ShouldReturnFalse() {
        // Arrange
        testRoom.setAvailable(false);

        when(reservationRepository.findByRequestId("test-request-id")).thenReturn(Optional.empty());
        when(roomService.getRoomById(1L)).thenReturn(testRoom);

        // Act & Assert
        assertThrows(RoomNotAvailableException.class,
                () -> reservationService.confirmAvailability(1L, confirmRequest)
        );
    }

    @Test
    void confirmAvailability_WithBookedDates_ShouldReturnFalse() {
        // Arrange
        when(reservationRepository.findByRequestId("test-request-id")).thenReturn(Optional.empty());
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(roomRepository.findAvailableRoomsForDates(any(), any()))
                .thenReturn(Arrays.asList()); // No available rooms
        when(reservationRepository.save(any(RoomReservation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        ConfirmAvailabilityResponse response = reservationService.confirmAvailability(1L, confirmRequest);

        // Assert
        assertFalse(response.isAvailable());
        assertNull(response.getReservationId());
        assertTrue(response.getMessage().contains("not available"));
        verify(reservationRepository).save(any(RoomReservation.class)); // Creates RELEASED reservation
    }

    @Test
    void releaseReservation_WithExistingReservation_ShouldReleaseIt() {
        // Arrange
        RoomReservation reservation = RoomReservation.builder()
                .id(1L)
                .room(testRoom)
                .bookingId(1L)
                .requestId("test-request-id")
                .startDate(confirmRequest.getStartDate())
                .endDate(confirmRequest.getEndDate())
                .status(RoomReservation.ReservationStatus.CONFIRMED)
                .build();

        ReleaseReservationRequest releaseRequest = ReleaseReservationRequest.builder()
                .requestId("test-request-id")
                .bookingId(1L)
                .build();

        when(reservationRepository.findByBookingId(1L)).thenReturn(Optional.of(reservation));

        // Act
        reservationService.releaseReservation(1L, releaseRequest);

        // Assert
        assertEquals(RoomReservation.ReservationStatus.RELEASED, reservation.getStatus());
        verify(reservationRepository).save(reservation);
    }

    @Test
    void releaseReservation_WithAlreadyReleasedReservation_ShouldDoNothing() {
        // Arrange
        RoomReservation reservation = RoomReservation.builder()
                .id(1L)
                .room(testRoom)
                .bookingId(1L)
                .requestId("test-request-id")
                .startDate(confirmRequest.getStartDate())
                .endDate(confirmRequest.getEndDate())
                .status(RoomReservation.ReservationStatus.RELEASED)
                .build();

        ReleaseReservationRequest releaseRequest = ReleaseReservationRequest.builder()
                .requestId("test-request-id")
                .bookingId(1L)
                .build();

        when(reservationRepository.findByBookingId(1L)).thenReturn(Optional.of(reservation));

        // Act
        reservationService.releaseReservation(1L, releaseRequest);

        // Assert
        assertEquals(RoomReservation.ReservationStatus.RELEASED, reservation.getStatus());
        verify(reservationRepository, never()).save(any()); // Should not save again
    }

    @Test
    void releaseReservation_WithNonExistentReservation_ShouldDoNothing() {
        // Arrange
        ReleaseReservationRequest releaseRequest = ReleaseReservationRequest.builder()
                .requestId("test-request-id")
                .bookingId(999L)
                .build();

        when(reservationRepository.findByBookingId(999L)).thenReturn(Optional.empty());

        // Act
        reservationService.releaseReservation(1L, releaseRequest);

        // Assert
        verify(reservationRepository, never()).save(any());
    }
}
