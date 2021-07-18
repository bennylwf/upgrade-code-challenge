package com.mybooking.campsite.v1.repository.dao;

import com.mybooking.campsite.v1.repository.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long>
{
    Booking findByConfirmCode(String confirmCode);
}
