package com.booking.hotel.repository;

import com.booking.hotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelIdAndAvailableTrue(Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.available = true AND " +
           "NOT EXISTS (SELECT rr FROM RoomReservation rr WHERE rr.room.id = r.id AND " +
           "rr.status <> 'RELEASED' AND " +
           "((rr.startDate <= :endDate AND rr.endDate >= :startDate)))")
    List<Room> findAvailableRoomsForDates(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

    @Query("SELECT r FROM Room r WHERE r.available = true AND r.hotel.id = :hotelId AND " +
           "NOT EXISTS (SELECT rr FROM RoomReservation rr WHERE rr.room.id = r.id AND " +
           "rr.status <> 'RELEASED' AND " +
           "((rr.startDate <= :endDate AND rr.endDate >= :startDate))) " +
           "ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findRecommendedRoomsForDates(@Param("hotelId") Long hotelId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);

    long countByAvailable(Boolean available);

    boolean existsByHotelIdAndNumber(Long hotelId, String number);
}
