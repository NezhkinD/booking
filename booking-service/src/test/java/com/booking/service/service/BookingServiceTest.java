package com.booking.service.service;

import com.booking.service.client.HotelClient;
import com.booking.service.dto.*;
import com.booking.service.entity.Booking;
import com.booking.service.entity.User;
import com.booking.service.exception.BookingException;
import com.booking.service.exception.ResourceNotFoundException;
import com.booking.service.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserService userService;

    @Mock
    private HotelClient hotelClient;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private BookingRequest validRequest;
    private Booking testBooking;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .role(User.Role.USER)
                .build();

        validRequest = BookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                .build();

        testBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(1L)
                .startDate(validRequest.getStartDate())
                .endDate(validRequest.getEndDate())
                .status(Booking.BookingStatus.CONFIRMED)
                .requestId("test-request-id")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createBooking_WithValidRequest_ShouldReturnConfirmedBooking() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.existsByRequestId(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking booking = i.getArgument(0);
            booking.setId(1L);
            return booking;
        });

        ConfirmAvailabilityResponse confirmResponse = ConfirmAvailabilityResponse.builder()
                .available(true)
                .reservationId(1L)
                .build();
        when(hotelClient.confirmAvailability(anyLong(), any())).thenReturn(confirmResponse);

        // Act
        BookingResponse response = bookingService.createBooking(validRequest, "testuser");

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(Booking.BookingStatus.CONFIRMED, response.getStatus());
        assertEquals("testuser", response.getUsername());
        verify(bookingRepository, times(2)).save(any(Booking.class)); // PENDING + CONFIRMED
        verify(hotelClient).confirmAvailability(eq(1L), any());
    }

    @Test
    void createBooking_WithAutoSelect_ShouldSelectRecommendedRoom() {
        // Arrange
        BookingRequest autoSelectRequest = BookingRequest.builder()
                .hotelId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(true)
                .build();

        RoomDTO recommendedRoom = RoomDTO.builder()
                .id(5L)
                .hotelId(1L)
                .number("101")
                .available(true)
                .timesBooked(2)
                .build();

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.existsByRequestId(any())).thenReturn(false);
        when(hotelClient.getRecommendedRooms(anyLong(), any(), any()))
                .thenReturn(Arrays.asList(recommendedRoom));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking booking = i.getArgument(0);
            booking.setId(1L);
            return booking;
        });

        ConfirmAvailabilityResponse confirmResponse = ConfirmAvailabilityResponse.builder()
                .available(true)
                .reservationId(1L)
                .build();
        when(hotelClient.confirmAvailability(anyLong(), any())).thenReturn(confirmResponse);

        // Act
        BookingResponse response = bookingService.createBooking(autoSelectRequest, "testuser");

        // Assert
        assertNotNull(response);
        assertEquals(5L, response.getRoomId()); // Selected room ID
        assertEquals(Booking.BookingStatus.CONFIRMED, response.getStatus());
        verify(hotelClient).getRecommendedRooms(eq(1L), any(), any());
    }

    @Test
    void createBooking_WithDuplicateRequestId_ShouldReturnExistingBooking() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.existsByRequestId(any())).thenReturn(true);
        when(bookingRepository.findByRequestId(any())).thenReturn(Optional.of(testBooking));

        // Act
        BookingResponse response = bookingService.createBooking(validRequest, "testuser");

        // Assert
        assertNotNull(response);
        assertEquals(testBooking.getId(), response.getId());
        verify(bookingRepository, never()).save(any());
        verify(hotelClient, never()).confirmAvailability(anyLong(), any());
    }

    @Test
    void createBooking_WhenRoomNotAvailable_ShouldCancelBooking() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.existsByRequestId(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking booking = i.getArgument(0);
            booking.setId(1L);
            return booking;
        });

        ConfirmAvailabilityResponse confirmResponse = ConfirmAvailabilityResponse.builder()
                .available(false)
                .build();
        when(hotelClient.confirmAvailability(anyLong(), any())).thenReturn(confirmResponse);

        // Act & Assert
        BookingException exception = assertThrows(BookingException.class, () ->
                bookingService.createBooking(validRequest, "testuser")
        );
        assertTrue(exception.getMessage().contains("not available"));

        // Verify booking was saved as CANCELLED (PENDING + CANCELLED + CANCELLED again)
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, atLeast(2)).save(captor.capture());
        assertEquals(Booking.BookingStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void createBooking_WhenHotelServiceFails_ShouldCompensate() {
        // Arrange
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.existsByRequestId(any())).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking booking = i.getArgument(0);
            booking.setId(1L);
            return booking;
        });
        when(hotelClient.confirmAvailability(anyLong(), any()))
                .thenThrow(new RuntimeException("Service unavailable"));

        // Act & Assert
        assertThrows(BookingException.class, () ->
                bookingService.createBooking(validRequest, "testuser")
        );

        // Verify compensation was attempted
        verify(hotelClient).releaseReservation(anyLong(), any());

        // Verify booking was saved as CANCELLED
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream()
                .anyMatch(b -> b.getStatus() == Booking.BookingStatus.CANCELLED));
    }

    @Test
    void createBooking_WithPastStartDate_ShouldThrowException() {
        // Arrange
        BookingRequest invalidRequest = BookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                .build();

        // Act & Assert
        BookingException exception = assertThrows(BookingException.class, () ->
                bookingService.createBooking(invalidRequest, "testuser")
        );
        assertTrue(exception.getMessage().contains("past"));
    }

    @Test
    void createBooking_WithEndDateBeforeStartDate_ShouldThrowException() {
        // Arrange
        BookingRequest invalidRequest = BookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(3))
                .autoSelect(false)
                .build();

        // Act & Assert
        BookingException exception = assertThrows(BookingException.class, () ->
                bookingService.createBooking(invalidRequest, "testuser")
        );
        assertTrue(exception.getMessage().contains("after start date"));
    }

    @Test
    void getUserBookings_ShouldReturnUserBookings() {
        // Arrange
        List<Booking> bookings = Arrays.asList(testBooking);
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(bookingRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(bookings);

        // Act
        List<BookingResponse> responses = bookingService.getUserBookings("testuser");

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testBooking.getId(), responses.get(0).getId());
    }

    @Test
    void getBookingById_WithValidId_ShouldReturnBooking() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act
        BookingResponse response = bookingService.getBookingById(1L, "testuser");

        // Assert
        assertNotNull(response);
        assertEquals(testBooking.getId(), response.getId());
    }

    @Test
    void getBookingById_WithWrongUser_ShouldThrowException() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        BookingException exception = assertThrows(BookingException.class, () ->
                bookingService.getBookingById(1L, "otheruser")
        );
        assertTrue(exception.getMessage().contains("Access denied"));
    }

    @Test
    void getBookingById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () ->
                bookingService.getBookingById(999L, "testuser")
        );
    }

    @Test
    void cancelBooking_WithValidId_ShouldCancelBooking() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        bookingService.cancelBooking(1L, "testuser");

        // Assert
        verify(bookingRepository).save(testBooking);
        verify(hotelClient).releaseReservation(anyLong(), any());
        assertEquals(Booking.BookingStatus.CANCELLED, testBooking.getStatus());
    }

    @Test
    void cancelBooking_WithAlreadyCancelledBooking_ShouldThrowException() {
        // Arrange
        testBooking.setStatus(Booking.BookingStatus.CANCELLED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        BookingException exception = assertThrows(BookingException.class, () ->
                bookingService.cancelBooking(1L, "testuser")
        );
        assertTrue(exception.getMessage().contains("already cancelled"));
    }

    @Test
    void cancelBooking_WithWrongUser_ShouldThrowException() {
        // Arrange
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        // Act & Assert
        BookingException exception = assertThrows(BookingException.class, () ->
                bookingService.cancelBooking(1L, "otheruser")
        );
        assertTrue(exception.getMessage().contains("Access denied"));
    }
}
