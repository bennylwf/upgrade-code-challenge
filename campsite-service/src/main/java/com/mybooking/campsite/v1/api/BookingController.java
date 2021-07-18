package com.mybooking.campsite.v1.api;

import com.mybooking.campsite.v1.exception.BadRequestException;
import com.mybooking.campsite.v1.rest.spec.api.BookingApi;
import com.mybooking.campsite.v1.rest.spec.model.BadRequest;
import com.mybooking.campsite.v1.rest.spec.model.Booking;
import com.mybooking.campsite.v1.service.BookingBizService;
import com.mybooking.campsite.v1.service.exception.BookingServiceException;
import com.mybooking.campsite.v1.service.exception.DateNotAvailabeException;
import com.mybooking.campsite.v1.shared.dto.BookingDto;
import com.mybooking.campsite.v1.shared.dto.UpdateResultDto;
import com.mybooking.campsite.v1.shared.util.BookingUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1")
public class BookingController implements BookingApi {

    private final NativeWebRequest request;  //this is request scope

    @Qualifier("cacheableBookingServiceImpl")
    private final BookingBizService bookingBizService;

    private final BookingUtil bookingUtil;

    public BookingController(NativeWebRequest request, BookingBizService bookingBizService,
                             BookingUtil bookingUtil) {
        this.request = request;
        this.bookingBizService = bookingBizService;
        this.bookingUtil = bookingUtil;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    @Override
    public ResponseEntity<Booking> addBooking(Booking booking) {
        try {
            BookingDto bookingDto = this.bookingBizService.addBooking(toBookingDto(booking));
            Booking rtnBooking = toBooking(bookingDto);
            return new ResponseEntity<>(rtnBooking, HttpStatus.OK);
        } catch (DateNotAvailabeException e) {
            throw newBadRequestException("b02", e.getMessage());
        } catch (BookingServiceException e) {
            throw newBadRequestException("b03", e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Void> cancelBooking(String confirmCode) {
        try {
            this.bookingBizService.deleteBooking(confirmCode);
            return new ResponseEntity<>(HttpStatus.OK);
        }
        catch (BookingServiceException e){
            throw newBadRequestException("b03", e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Booking> getBookingByConfirmCode(String confirmCode) {
        try {
            BookingDto bookingDto = this.bookingBizService.getBookingByConfirmCode(confirmCode);
            Booking rtnBooking = toBooking(bookingDto);
            return new ResponseEntity<>(rtnBooking, HttpStatus.OK);
        } catch (BookingServiceException e) {
            throw newBadRequestException("b03", e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Booking> updateBooking(String confirmCode, Booking booking) {
        BookingDto bookingDto = toBookingDto(booking);
        bookingDto.setConfirmCode(confirmCode);
        try {
            UpdateResultDto updateResultDto = this.bookingBizService.updateBooking(bookingDto);
            Booking rtnBooking = toBooking(updateResultDto.getUpdatedBookingDto());
            return new ResponseEntity<>(rtnBooking, HttpStatus.OK);
        } catch (DateNotAvailabeException e) {
            throw newBadRequestException("b02", e.getMessage());
        } catch (BookingServiceException e) {
            throw newBadRequestException("b03", e.getMessage());
        }
    }

    private BookingDto toBookingDto(Booking booking) {
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate checkOutDate = booking.getCheckOutDate();
        if (!this.bookingUtil.isValidCheckInCheckOutDate(checkInDate.toString(), checkOutDate.toString(), true)) {
            throw newBadRequestException("b01", "Invalid booking checkin/checkout date");
        }
        BookingDto bookingDto = new BookingDto();
        bookingDto.setBookingDates(this.bookingUtil.getBookingDates(checkInDate, checkOutDate));
        bookingDto.setEmail(booking.getEmail());
        bookingDto.setFirstName(booking.getFirstName());
        bookingDto.setLastName(booking.getLastName());
        bookingDto.setConfirmCode(booking.getConfirmCode());
        return bookingDto;
    }

    private Booking toBooking(BookingDto bookingDto) {
        Booking booking = new Booking();
        List<LocalDate> bookingDates = bookingDto.getBookingDates();
        if (bookingDates != null && bookingDates.size() > 0) {
            Collections.sort(bookingDates);
            booking.setCheckInDate(bookingDates.get(0));
            booking.setCheckOutDate(bookingDates.get(bookingDates.size() - 1).plusDays(1));
        }
        booking.email(bookingDto.getEmail());
        booking.firstName(bookingDto.getFirstName());
        booking.lastName(bookingDto.getLastName());
        booking.setConfirmCode(bookingDto.getConfirmCode());
        return booking;
    }

    private BadRequestException newBadRequestException(String errCode, String message) {
        final BadRequest badRequest = new BadRequest()
                .message(message)
                .errorCode(errCode);
        return new BadRequestException(HttpStatus.BAD_REQUEST, badRequest);
    }
}
