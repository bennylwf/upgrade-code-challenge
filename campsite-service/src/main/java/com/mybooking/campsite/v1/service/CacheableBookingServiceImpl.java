package com.mybooking.campsite.v1.service;

import com.mybooking.campsite.v1.service.exception.BookingServiceException;
import com.mybooking.campsite.v1.service.exception.DateNotAvailabeException;
import com.mybooking.campsite.v1.service.exception.NoPickupDateException;
import com.mybooking.campsite.v1.shared.dto.BookingDto;
import com.mybooking.campsite.v1.shared.dto.DeleteResultDto;
import com.mybooking.campsite.v1.shared.dto.UpdateResultDto;
import org.apache.commons.lang3.RandomStringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Qualifier("cacheableBookingServiceImpl")
public class CacheableBookingServiceImpl implements BookingBizService {


    private static final int TTL = 31;
    public static final int MAX_DAYS = 30;
    private BookingDbService bookingDbService;
    private RedissonClient redisson;

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );


    private final RMapCache<LocalDate, String> cachedBookingDates;  //Date -> Confirm Code

    public CacheableBookingServiceImpl(@Qualifier("bookingDbServiceImpl") BookingDbService bookingDbService,
                                       @Autowired RedissonClient redisson) {
        this.bookingDbService = bookingDbService;
        this.redisson = redisson;
        this.cachedBookingDates = this.redisson.getMapCache("cache:bookingDates");
    }

    @Override
    public BookingDto addBooking(BookingDto bookingDto) {
        return wrapInRLock(this::doAddBooking, bookingDto);
    }

    private BookingDto doAddBooking(BookingDto bookingDto) {

        if (bookingDto.getBookingDates() == null || bookingDto.getBookingDates().size() == 0) {
            throw new NoPickupDateException("You didn't pick up any dates");
        }

        if (!isDateAvailable(bookingDto.getBookingDates())) {
            throw new DateNotAvailabeException("One of the dates are not available: " +
                    bookingDto.getBookingDates() + ", please retry and pick up other dates.");
        }
        String confirmCode = RandomStringUtils.random(10, true, true);
        bookingDto.setConfirmCode(confirmCode); //random is expensive, gen the confirm code before transaction.
        bookingDto.getBookingDates().forEach(localDate -> this.cachedBookingDates.fastPut(localDate, confirmCode, TTL, TimeUnit.DAYS));
        //return this.wrapInRLock(bookingDbService::addBooking, bookingDto);
        BookingDto rtnBookingInfo = bookingDbService.addBooking(bookingDto);
        logger.info("Finish adding booking");
        return rtnBookingInfo;

    }

    @Override
    public UpdateResultDto updateBooking(BookingDto bookingDto) {
       return wrapInRLock(this::doUpdateBooking, bookingDto);
    }

    private UpdateResultDto doUpdateBooking(BookingDto bookingDto) {

        if (bookingDto.getBookingDates() == null || bookingDto.getBookingDates().size() == 0) {
            throw new NoPickupDateException("You didn't pick up any dates");
        }
        if (!this.cachedBookingDates.containsValue(bookingDto.getConfirmCode())) {
            throw new BookingServiceException("Can't find booking by the confirmCode " + bookingDto.getConfirmCode());
        }

        if (!isDateAvailableForCurrentBooking(bookingDto.getBookingDates(), bookingDto.getConfirmCode())) {
            throw new DateNotAvailabeException("One of the dates are not available: " +
                    bookingDto.getBookingDates() + ", please retry and pick up other dates.");
        }

        bookingDto.getBookingDates().forEach(localDate -> this.cachedBookingDates.fastPut(
                localDate, bookingDto.getConfirmCode(), TTL, TimeUnit.DAYS));

        //UpdateResultDto updateResultDto = this.wrapInRLock(bookingDbService::updateBooking, bookingDto);
        UpdateResultDto updateResultDto = bookingDbService.updateBooking(bookingDto);

        this.cachedBookingDates.fastRemove(updateResultDto.getDeletedDates().toArray(new LocalDate[0]));
        logger.info("Finish update booking: " + bookingDto.getConfirmCode());
        return updateResultDto;
    }

    private <T, R> R wrapInRLock(Function<T, R> func, T t) {
        RLock lock = this.redisson.getLock("lock:writeBooking");
        lock.lock(10, TimeUnit.SECONDS);
        try {
            return func.apply(t);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public DeleteResultDto deleteBooking(String confirmCode) {
        if (!this.cachedBookingDates.containsValue(confirmCode)) {
            throw new BookingServiceException("Can't find booking by the confirmCode " + confirmCode);
        }
        DeleteResultDto deleteResultDto = bookingDbService.deleteBooking(confirmCode);
        this.cachedBookingDates.fastRemove(deleteResultDto.getDeletedDates().toArray(new LocalDate[0]));
        return deleteResultDto;
    }

    @Override
    public BookingDto getBookingByConfirmCode(String confirmCode) {
        return wrapInRLock(this::doGetBookingByConfirmCode, confirmCode);
    }

    private BookingDto doGetBookingByConfirmCode(String confirmCode) {
        BookingDto bookingDto = bookingDbService.getBookingByConfirmCode(confirmCode);
        syncCacheByDb(bookingDto);
        return bookingDto;
    }

    private void syncCacheByDb(BookingDto bookingDto) {
        LocalDate today = LocalDate.now();
        List<LocalDate> dbDates = bookingDto.getBookingDates().stream().filter(
                //find the days in next 30 days to cache
                theDate -> isBetween(today.plusDays(1), today.plusDays(MAX_DAYS), theDate)).collect(Collectors.toList());
        Collections.sort(bookingDto.getBookingDates());
        List<LocalDate> cachedDates = this.cachedBookingDates.entrySet().stream().filter(
                e -> e.getValue().equalsIgnoreCase(bookingDto.getConfirmCode())).map(Map.Entry::getKey)
                .collect(Collectors.toList());
        this.cachedBookingDates.fastRemove(cachedDates.toArray(cachedDates.toArray(new LocalDate[0])));
        dbDates.forEach(localDate -> this.cachedBookingDates.fastPut(localDate, bookingDto.getConfirmCode(),
                TTL, TimeUnit.DAYS));
    }

    private boolean isDateAvailableForCurrentBooking(List<LocalDate> localDates, String confirmCode) {
        logger.info("Checking date: " + localDates + "for updating booking: " + confirmCode);
        if (localDates == null || localDates.size() ==0 ) {
            return false;
        }

        for (int i = 0; i < localDates.size(); i++) {
            String cCode = this.cachedBookingDates.get(localDates.get(i));
            if (cCode != null && !cCode.equalsIgnoreCase(confirmCode)) {
                //Owned by someone else
                return false;
            }
        }
        return true;
    }

    private boolean isDateAvailable(List<LocalDate> localDates) {
        logger.info("Checking date: " + localDates + " for making new reservation.");
        if (localDates == null || localDates.size() ==0 ) {
            return false;
        }
        if (localDates.stream().anyMatch(localDate -> !isDateAvailable(localDate))) {
            return false;
        }
        return true;
    }

    private boolean isDateAvailable(LocalDate localDate) {
        LocalDate today = LocalDate.now();
        if (localDate.isBefore(today.plusDays(1)) || localDate.isAfter(today.plusDays(MAX_DAYS))) {
            return false;
        }
        if (this.cachedBookingDates.containsKey(localDate)) {
            return false;
        }
        return true;
    }


    @Override
    public List<LocalDate> getAvailableDatesForCurrentBooking(LocalDate startDate, LocalDate endDate, String confirmCode) throws BookingServiceException {
            List<LocalDate> avDates = getAvailableDates(startDate, endDate);
            List<LocalDate> ownedDates = this.cachedBookingDates.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(confirmCode)).map(Map.Entry::getKey)
                    .filter(localDate -> !avDates.contains(localDate) && isBetween(startDate, endDate, localDate)).collect(Collectors.toList());
            avDates.addAll(ownedDates);
            return avDates;
    }

    private boolean isBetween(LocalDate startDate, LocalDate endDate, LocalDate theDate) {
        if (theDate.isEqual(startDate) || theDate.isAfter(startDate) || theDate.isEqual(endDate) || theDate.isBefore(endDate)) {
            return true;
        }
        return false;
    }

    public List<LocalDate> getAvailableDates(LocalDate startDate, LocalDate endDate) throws BookingServiceException {

           if (startDate.isAfter(endDate)) {
                throw new BookingServiceException("StartDate can't be after endDate");
            }
            List<LocalDate> availableDates = new ArrayList<>();
            LocalDate theDate = startDate;
            while (!theDate.isAfter(endDate)) {
                if (isDateAvailable(theDate)) {
                    availableDates.add(theDate);
                }
                theDate = theDate.plusDays(1);
            }
            return availableDates;
    }

    public RMapCache<LocalDate, String> getCachedBookingDates() {
        return cachedBookingDates;
    }


}
