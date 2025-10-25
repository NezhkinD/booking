package com.booking.hotel.service;

import com.booking.hotel.dto.RoomCreateRequest;
import com.booking.hotel.dto.RoomDTO;
import com.booking.hotel.entity.Hotel;
import com.booking.hotel.entity.Room;
import com.booking.hotel.exception.ResourceNotFoundException;
import com.booking.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelService hotelService;

    @InjectMocks
    private RoomService roomService;

    private Hotel testHotel;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("123 Test St")
                .createdAt(LocalDateTime.now())
                .build();

        testRoom = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createRoom_WithValidRequest_ShouldReturnRoomDTO() {
        // Arrange
        RoomCreateRequest request = RoomCreateRequest.builder()
                .hotelId(1L)
                .number("202")
                .build();

        when(hotelService.getHotelById(1L)).thenReturn(testHotel);
        when(roomRepository.save(any(Room.class))).thenAnswer(i -> {
            Room room = i.getArgument(0);
            room.setId(2L);
            room.setCreatedAt(LocalDateTime.now());
            return room;
        });

        // Act
        RoomDTO result = roomService.createRoom(request);

        // Assert
        assertNotNull(result);
        assertEquals("202", result.getNumber());
        assertEquals(1L, result.getHotelId());
        assertTrue(result.getAvailable());
        assertEquals(0, result.getTimesBooked());
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    void createRoom_WithNonExistentHotel_ShouldThrowException() {
        // Arrange
        RoomCreateRequest request = RoomCreateRequest.builder()
                .hotelId(999L)
                .number("202")
                .build();

        when(hotelService.getHotelById(999L))
                .thenThrow(new ResourceNotFoundException("Hotel not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> roomService.createRoom(request)
        );
        verify(roomRepository, never()).save(any());
    }

    @Test
    void getAvailableRooms_ShouldReturnAvailableRooms() {
        // Arrange
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);
        List<Room> rooms = Arrays.asList(testRoom);

        when(roomRepository.findAvailableRoomsForDates(startDate, endDate)).thenReturn(rooms);

        // Act
        List<RoomDTO> result = roomService.getAvailableRooms(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("101", result.get(0).getNumber());
    }

    @Test
    void getRecommendedRooms_ShouldReturnSortedRooms() {
        // Arrange
        Long hotelId = 1L;
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = LocalDate.now().plusDays(3);

        Room room1 = Room.builder()
                .id(1L)
                .hotel(testHotel)
                .number("101")
                .available(true)
                .timesBooked(5)
                .build();

        Room room2 = Room.builder()
                .id(2L)
                .hotel(testHotel)
                .number("102")
                .available(true)
                .timesBooked(2)
                .build();

        List<Room> rooms = Arrays.asList(room2, room1); // Already sorted by repository

        when(roomRepository.findRecommendedRoomsForDates(hotelId, startDate, endDate))
                .thenReturn(rooms);

        // Act
        List<RoomDTO> result = roomService.getRecommendedRooms(hotelId, startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getTimesBooked()); // Less booked room first
        assertEquals(5, result.get(1).getTimesBooked());
    }

    @Test
    void getRoomById_WithValidId_ShouldReturnRoom() {
        // Arrange
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        // Act
        Room result = roomService.getRoomById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("101", result.getNumber());
    }

    @Test
    void getRoomById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> roomService.getRoomById(999L)
        );
    }
}
