package com.mybooking.campsite.v1.repository.dao;

import com.mybooking.campsite.v1.repository.domain.BookingDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingDateRepository extends JpaRepository<BookingDate, Long>
{
}
