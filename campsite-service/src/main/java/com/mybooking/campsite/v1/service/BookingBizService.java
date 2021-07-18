package com.mybooking.campsite.v1.service;

import com.mybooking.campsite.v1.service.exception.BookingServiceException;

import java.time.LocalDate;
import java.util.List;

public interface BookingBizService extends BookingService{

    List<LocalDate> getAvailableDates(LocalDate startDate, LocalDate endDate) throws BookingServiceException;

    List<LocalDate> getAvailableDatesForCurrentBooking(LocalDate startDate, LocalDate endDate, String confirmCode) throws BookingServiceException;
}
