package com.booking.hotel.service;

import com.booking.hotel.dto.HotelCreateRequest;
import com.booking.hotel.dto.HotelDTO;
import com.booking.hotel.entity.Hotel;
import com.booking.hotel.exception.ResourceNotFoundException;
import com.booking.hotel.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private HotelService hotelService;

    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        testHotel = Hotel.builder()
                .id(1L)
                .name("Grand Hotel")
                .address("123 Main St, Moscow")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createHotel_WithValidRequest_ShouldReturnHotelDTO() {
        // Arrange
        HotelCreateRequest request = HotelCreateRequest.builder()
                .name("New Hotel")
                .address("456 New St")
                .build();

        when(hotelRepository.save(any(Hotel.class))).thenAnswer(i -> {
            Hotel hotel = i.getArgument(0);
            hotel.setId(2L);
            hotel.setCreatedAt(LocalDateTime.now());
            return hotel;
        });

        // Act
        HotelDTO result = hotelService.createHotel(request);

        // Assert
        assertNotNull(result);
        assertEquals("New Hotel", result.getName());
        assertEquals("456 New St", result.getAddress());
        assertNotNull(result.getCreatedAt());
        verify(hotelRepository).save(any(Hotel.class));
    }

    @Test
    void getAllHotels_ShouldReturnAllHotels() {
        // Arrange
        Hotel hotel2 = Hotel.builder()
                .id(2L)
                .name("Budget Inn")
                .address("789 Budget Ave")
                .createdAt(LocalDateTime.now())
                .build();

        List<Hotel> hotels = Arrays.asList(testHotel, hotel2);
        when(hotelRepository.findAll()).thenReturn(hotels);

        // Act
        List<HotelDTO> result = hotelService.getAllHotels();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Grand Hotel", result.get(0).getName());
        assertEquals("Budget Inn", result.get(1).getName());
    }

    @Test
    void getAllHotels_WhenEmpty_ShouldReturnEmptyList() {
        // Arrange
        when(hotelRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<HotelDTO> result = hotelService.getAllHotels();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getHotelById_WithValidId_ShouldReturnHotel() {
        // Arrange
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(testHotel));

        // Act
        Hotel result = hotelService.getHotelById(1L);

        // Assert
        assertNotNull(result);
        assertEquals("Grand Hotel", result.getName());
        assertEquals("123 Main St, Moscow", result.getAddress());
    }

    @Test
    void getHotelById_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(hotelRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> hotelService.getHotelById(999L)
        );
        assertTrue(exception.getMessage().contains("Hotel not found"));
    }
}
