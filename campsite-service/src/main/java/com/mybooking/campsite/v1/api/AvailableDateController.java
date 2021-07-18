package com.mybooking.campsite.v1.api;

import com.mybooking.campsite.v1.exception.BadRequestException;
import com.mybooking.campsite.v1.rest.spec.api.AvailableDateApi;
import com.mybooking.campsite.v1.rest.spec.model.AvailableDate;
import com.mybooking.campsite.v1.rest.spec.model.BadRequest;
import com.mybooking.campsite.v1.service.BookingBizService;
import com.mybooking.campsite.v1.shared.util.BookingUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1")
public class AvailableDateController implements AvailableDateApi {

    private final NativeWebRequest request;  //this is request scope

    @Qualifier("cacheableBookingServiceImpl")
    private final BookingBizService bookingBizService;

    private final BookingUtil bookingUtil;

    public AvailableDateController(NativeWebRequest request, BookingBizService bookingBizService
            , BookingUtil bookingUtil) {
        this.request = request;
        this.bookingBizService = bookingBizService;
        this.bookingUtil = bookingUtil;
    }

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return Optional.ofNullable(request);
    }

    @Override
    public ResponseEntity<AvailableDate> getAvailableDate(String checkInDate, String checkOutDate, String confirmCode) {

        if (!this.bookingUtil.isValidCheckInCheckOutDate(checkInDate, checkOutDate, false)) {
            throw newBadRequestException("a01", "Invalid inquery checkin/checkout date");
        }

        LocalDate startDate = LocalDate.parse(checkInDate);
        LocalDate endDate = LocalDate.parse(checkOutDate).minusDays(1);

        List<LocalDate> avaDates = new ArrayList<>();
        if (confirmCode != null) {
            if (confirmCode.length() != 10) {
                throw newBadRequestException("a02", "Invalid confirmation code");
            }
            avaDates = this.bookingBizService.getAvailableDatesForCurrentBooking(startDate, endDate, confirmCode);
        } else {
            avaDates = this.bookingBizService.getAvailableDates(startDate, endDate);
        }
        AvailableDate availableDate = new AvailableDate().avaDates(avaDates);
        return new ResponseEntity<>(availableDate, HttpStatus.OK);
    }

    private BadRequestException newBadRequestException(String errCode, String message) {
        final BadRequest badRequest = new BadRequest()
                .message(message)
                .errorCode(errCode);
        return new BadRequestException(HttpStatus.BAD_REQUEST, badRequest);
    }
}
