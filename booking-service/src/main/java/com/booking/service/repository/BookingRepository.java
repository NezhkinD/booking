package com.booking.service.repository;

import com.booking.service.entity.Booking;
import com.booking.service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    Page<Booking> findByUser(User user, Pageable pageable);

    Optional<Booking> findByRequestId(String requestId);

    boolean existsByRequestId(String requestId);

    long countByStatus(Booking.BookingStatus status);
}
