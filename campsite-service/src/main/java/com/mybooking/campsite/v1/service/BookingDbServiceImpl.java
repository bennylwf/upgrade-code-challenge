package com.mybooking.campsite.v1.service;

import com.mybooking.campsite.v1.repository.dao.BookingDateRepository;
import com.mybooking.campsite.v1.repository.dao.BookingRepository;
import com.mybooking.campsite.v1.repository.domain.Booking;
import com.mybooking.campsite.v1.repository.domain.BookingDate;
import com.mybooking.campsite.v1.service.exception.BookingServiceException;
import com.mybooking.campsite.v1.shared.dto.BookingDto;
import com.mybooking.campsite.v1.shared.dto.DeleteResultDto;
import com.mybooking.campsite.v1.shared.dto.UpdateResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Qualifier("bookingDbServiceImpl")
public class BookingDbServiceImpl implements BookingDbService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDateRepository bookingDateRepository;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingDto addBooking(BookingDto bookingDto) {

        final Booking booking = toBooking(bookingDto);
        Booking savedBooking = bookingRepository.save(booking);
        if (savedBooking.getId() <= 0) {
            throw new RuntimeException("Failed to save booking");
        }
        return bookingDto;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public UpdateResultDto updateBooking(BookingDto bookingDto) {

        Booking booking = bookingRepository.findByConfirmCode(bookingDto.getConfirmCode());
        if ( null == booking) {
            throw new BookingServiceException("Can't find booking by the confirmCode " + bookingDto.getConfirmCode());
        }
        List<BookingDate> bookDateToBeDeleted = booking.getBookingDates().stream()
                .filter(bookingDate -> !bookingDto.getBookingDates().contains(bookingDate.getRevDate()) )
                .collect(Collectors.toList());

        Set<LocalDate> curLocalDateSet = booking.getBookingDates().stream().map(BookingDate::getRevDate).collect(Collectors.toSet());
        List<BookingDate> bookDateToBeAdded = bookingDto.getBookingDates().stream()
                .filter(localDate -> !curLocalDateSet.contains(localDate))
                .map(localDate -> toBookingDate(localDate, booking))
                .collect(Collectors.toList());

        //Keep the overlap dates to avoid the hibernate doing insertion before deletion
        booking.getBookingDates().removeAll(bookDateToBeDeleted);
        booking.getBookingDates().addAll(bookDateToBeAdded);

        bookingDateRepository.deleteAll(bookDateToBeDeleted);
        bookingDateRepository.saveAll(bookDateToBeAdded);

        //Booking savedBooking = bookingRepository.save(booking);
        UpdateResultDto updatedResult = new UpdateResultDto();
        updatedResult.setUpdatedBookingDto(toBookingDto(booking));
        updatedResult.setDeletedDates(
                bookDateToBeDeleted.stream().map(BookingDate::getRevDate).collect(Collectors.toList()));
        return updatedResult ;
    }

    @Transactional
    public DeleteResultDto deleteBooking(String confirmCode) {
        Booking booking = bookingRepository.findByConfirmCode(confirmCode);
        if ( null == booking) {
            throw new BookingServiceException("Can't find booking by the confirmCode " + confirmCode);
        }
        DeleteResultDto deleteResultDto = new DeleteResultDto();
        deleteResultDto.setDeletedDates(
                booking.getBookingDates().stream().map(BookingDate::getRevDate).collect(Collectors.toList()));
        bookingRepository.delete(booking);
        return deleteResultDto;

    }

    @Override
    @Transactional
    public BookingDto getBookingByConfirmCode(String confirmCode) {
        Booking booking = bookingRepository.findByConfirmCode(confirmCode);
        if ( null == booking) {
            throw new BookingServiceException("Can't find booking by the confirmCode " + confirmCode);
        }
        return toBookingDto(booking);
    }

    private BookingDto toBookingDto(Booking booking) {

        final BookingDto bookingDto = new BookingDto();
        bookingDto.setEmail(booking.getEmail());
        bookingDto.setFirstName(booking.getFirstName());
        bookingDto.setLastName(booking.getLastName());
        bookingDto.setConfirmCode(booking.getConfirmCode());

        List<LocalDate> localDates = booking.getBookingDates().stream()
                .map(BookingDate::getRevDate).collect(Collectors.toList());
        bookingDto.setBookingDates(localDates);
        return bookingDto;
    }

    private Booking toBooking(BookingDto bookingDto) {
        final Booking booking = new Booking();
        booking.setEmail(bookingDto.getEmail());
        booking.setFirstName(bookingDto.getFirstName());
        booking.setLastName(bookingDto.getLastName());

        List<BookingDate> bookingDates = bookingDto.getBookingDates().stream()
                .map(localDate -> toBookingDate(localDate, booking)).collect(Collectors.toList());
        booking.setBookingDates(bookingDates);
        booking.setConfirmCode(bookingDto.getConfirmCode());
        return booking;
    }

    private BookingDate toBookingDate(LocalDate localDate, Booking booking) {
        BookingDate bookingDate = new BookingDate();
        bookingDate.setBooking(booking);
        bookingDate.setRevDate(localDate);
        return bookingDate;
    }

}
