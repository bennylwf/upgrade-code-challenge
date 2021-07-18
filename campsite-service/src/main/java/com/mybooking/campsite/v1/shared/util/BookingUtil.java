package com.mybooking.campsite.v1.shared.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class BookingUtil {

    public boolean isValidCheckInCheckOutDate(String checkInDate, String checkOutDate, boolean forBooking) {
        if (checkInDate == null || checkOutDate == null || checkInDate.length() <10 || checkOutDate.length() < 10) {
            return false;
        }
        LocalDate startDate = LocalDate.parse(checkInDate);
        LocalDate endDate = LocalDate.parse(checkOutDate).minusDays(1); //exclusive checkoutDate
        LocalDate today = LocalDate.now();

        if (startDate.isAfter(endDate) || startDate.isBefore(today) || startDate.isEqual(today)) {
            return false;
        }
        if(endDate.isAfter(today.plusDays(30))) { // max 30 days
            return false;
        }

        if (forBooking) {
            if(endDate.isAfter(startDate.plusDays(2))) { //You can book max 3 days
                return false;
            }
        }
        return true;
    }

    public List<LocalDate> getBookingDates(LocalDate checkInDate, LocalDate checkOutDate) {
        List<LocalDate> bookingDates = new ArrayList<>();
        LocalDate theDate = checkInDate;
        while (theDate.isBefore(checkOutDate)) {
            bookingDates.add(theDate);
            theDate = theDate.plusDays(1);
        }
        return bookingDates;

    }
}
