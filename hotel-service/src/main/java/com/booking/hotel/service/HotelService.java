package com.booking.hotel.service;

import com.booking.hotel.dto.HotelCreateRequest;
import com.booking.hotel.dto.HotelDTO;
import com.booking.hotel.dto.HotelStatisticsDTO;
import com.booking.hotel.entity.Hotel;
import com.booking.hotel.entity.Room;
import com.booking.hotel.exception.ResourceNotFoundException;
import com.booking.hotel.repository.HotelRepository;
import com.booking.hotel.repository.RoomReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomReservationRepository reservationRepository;

    @Transactional
    public HotelDTO createHotel(HotelCreateRequest request) {
        log.debug("Creating hotel: {}", request.getName());

        Hotel hotel = Hotel.builder()
                .name(request.getName())
                .address(request.getAddress())
                .build();

        hotel = hotelRepository.save(hotel);
        log.info("Hotel created successfully: id={}, name={}", hotel.getId(), hotel.getName());

        return mapToDTO(hotel);
    }

    @Transactional(readOnly = true)
    public List<HotelDTO> getAllHotels() {
        log.debug("Fetching all hotels");

        return hotelRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Hotel getHotelById(Long hotelId) {
        return hotelRepository.findById(hotelId)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with ID: " + hotelId));
    }

    @Transactional(readOnly = true)
    public List<HotelStatisticsDTO> getAllHotelStatistics() {
        log.debug("Fetching statistics for all hotels");

        return hotelRepository.findAll()
                .stream()
                .map(this::calculateHotelStatistics)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HotelStatisticsDTO getHotelStatistics(Long hotelId) {
        log.debug("Fetching statistics for hotel: {}", hotelId);

        Hotel hotel = getHotelById(hotelId);
        return calculateHotelStatistics(hotel);
    }

    private HotelStatisticsDTO calculateHotelStatistics(Hotel hotel) {
        List<Room> rooms = hotel.getRooms();
        long totalRooms = rooms.size();
        long availableRooms = rooms.stream().filter(Room::getAvailable).count();
        long unavailableRooms = totalRooms - availableRooms;

        double averageTimesBooked = rooms.stream()
                .mapToInt(Room::getTimesBooked)
                .average()
                .orElse(0.0);

        long totalReservations = rooms.stream()
                .mapToLong(room -> reservationRepository.countByRoomIdAndStatus(
                        room.getId(),
                        com.booking.hotel.entity.RoomReservation.ReservationStatus.CONFIRMED))
                .sum();

        double occupancyRate = totalRooms > 0 ? (double) unavailableRooms / totalRooms * 100 : 0.0;

        return HotelStatisticsDTO.builder()
                .hotelId(hotel.getId())
                .hotelName(hotel.getName())
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .unavailableRooms(unavailableRooms)
                .averageTimesBooked(Math.round(averageTimesBooked * 100.0) / 100.0)
                .totalReservations(totalReservations)
                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                .build();
    }

    private HotelDTO mapToDTO(Hotel hotel) {
        return HotelDTO.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .createdAt(hotel.getCreatedAt())
                .build();
    }
}
