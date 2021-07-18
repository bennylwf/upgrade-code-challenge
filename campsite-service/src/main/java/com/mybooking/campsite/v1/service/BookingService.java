package com.mybooking.campsite.v1.service;

import com.mybooking.campsite.v1.shared.dto.BookingDto;
import com.mybooking.campsite.v1.shared.dto.DeleteResultDto;
import com.mybooking.campsite.v1.shared.dto.UpdateResultDto;

public interface BookingService {

    BookingDto addBooking(BookingDto bookingDto);
    UpdateResultDto updateBooking(BookingDto bookingDto);
    DeleteResultDto deleteBooking(String confirmCode);
    BookingDto getBookingByConfirmCode(String confirmCode);
}
