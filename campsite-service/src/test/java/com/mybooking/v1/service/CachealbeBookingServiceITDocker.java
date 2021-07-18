package com.mybooking.v1.service;

import com.mybooking.campsite.Application;
import com.mybooking.campsite.v1.repository.dao.BookingDateRepository;
import com.mybooking.campsite.v1.repository.dao.BookingRepository;
import com.mybooking.campsite.v1.service.BookingDbServiceImpl;
import com.mybooking.campsite.v1.service.CacheableBookingServiceImpl;
import com.mybooking.campsite.v1.service.exception.DateNotAvailabeException;
import com.mybooking.campsite.v1.service.exception.NoPickupDateException;
import com.mybooking.campsite.v1.shared.dto.BookingDto;
import com.mybooking.campsite.v1.shared.dto.UpdateResultDto;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class)
@AutoConfigureMockMvc
//@TestPropertySource(
//        locations = "classpath:application-integrationtest.yml")
public class CachealbeBookingServiceITDocker {

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Autowired
    @Qualifier("cacheableBookingServiceImpl")
    private CacheableBookingServiceImpl cacheableBookingService;

    @Autowired
    @Qualifier("bookingDbServiceImpl")
    private BookingDbServiceImpl bookingDbServiceImpl;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDateRepository bookingDateRepository;

    @Autowired
    private RedissonClient redisson;

    @Before
    public void resetAllData() {
        bookingDateRepository.deleteAll();
        bookingRepository.deleteAll();
        this.cacheableBookingService.getCachedBookingDates().delete();
    }

    @Test
    public void testAddBooking_Successful() {
        LocalDate today = LocalDate.now();

        BookingDto booking1 = createBookingDto(today.plusDays(1), today.plusDays(2), today.plusDays(3));

        BookingDto rtnBooking1 = cacheableBookingService.addBooking(booking1);

        BookingDto booking2 = createBookingDto(today.plusDays(5), today.plusDays(6));
        BookingDto rtnBooking2 = cacheableBookingService.addBooking(booking2);

        Assert.assertEquals(5, cacheableBookingService.getCachedBookingDates().size());
        Assert.assertEquals(5, bookingDateRepository.findAll().size());
    }

    @Test
    public void testAddBooking_With_Already_Reserved_Date() {
        LocalDate today = LocalDate.now();

        BookingDto booking1 = createBookingDto(today.plusDays(1), today.plusDays(2), today.plusDays(3));

        BookingDto rtnBooking1 = cacheableBookingService.addBooking(booking1);

        BookingDto booking2 = createBookingDto(today.plusDays(2), today.plusDays(3));
        try {
            BookingDto rtnBooking2 = cacheableBookingService.addBooking(booking2);
        } catch (DateNotAvailabeException e) {
            //expected
        }
        Assert.assertEquals(3, cacheableBookingService.getCachedBookingDates().size());
        Assert.assertEquals(3, bookingDateRepository.findAll().size());
    }


    @Test
    public void testEditBooking_Successful() {
        LocalDate today = LocalDate.now();

        BookingDto booking1 = createBookingDto(today.plusDays(1), today.plusDays(2), today.plusDays(3));
        BookingDto rtnBooking1 = cacheableBookingService.addBooking(booking1);

        rtnBooking1.setBookingDates(Arrays.asList(today.plusDays(1), today.plusDays(2)));
        cacheableBookingService.updateBooking(rtnBooking1);
        Assert.assertEquals(2, cacheableBookingService.getCachedBookingDates().size());
        Assert.assertEquals(2, bookingDateRepository.findAll().size());

        rtnBooking1.setBookingDates(Arrays.asList(today.plusDays(5), today.plusDays(6), today.plusDays(7)));
        cacheableBookingService.updateBooking(rtnBooking1);
        Assert.assertEquals(3, cacheableBookingService.getCachedBookingDates().size());
        Assert.assertEquals(3, bookingDateRepository.findAll().size());

    }

    //@Test(expected = DateNotAvailabeException.class)
    @Test
    public void testEditBooking_With_Already_Reserved_Date() {
        LocalDate today = LocalDate.now();

        BookingDto booking1 = createBookingDto(today.plusDays(1), today.plusDays(2), today.plusDays(3));
        BookingDto rtnBooking1 = cacheableBookingService.addBooking(booking1);

        BookingDto booking2 = createBookingDto(today.plusDays(5), today.plusDays(6));
        BookingDto rtnBooking2 = cacheableBookingService.addBooking(booking2);

        try {
            rtnBooking1.setBookingDates(Arrays.asList(today.plusDays(3), today.plusDays(4), today.plusDays(5)));
            cacheableBookingService.updateBooking(rtnBooking1);
        } catch (DateNotAvailabeException e) {
            //expected
        }
        Assert.assertEquals(5, cacheableBookingService.getCachedBookingDates().size());
        Assert.assertEquals(5, bookingDateRepository.findAll().size());

    }

    @Test
    public void testConcurrent_Add_Edit() {

        List<BookingDto> bookingDtos = null;
        ExecutorService executorService = Executors.newFixedThreadPool(30);

        //Simulate concurrent 'add'
        List<CompletableFuture<BookingDto>> addBookingFutures = IntStream.rangeClosed(1, 50).parallel()
                .mapToObj(n -> CompletableFuture.supplyAsync(() -> this.makeBooking(), executorService)
                        .exceptionally(this::handleAddBookingException ))
                .collect(Collectors.toList());

        CompletableFuture<Void> generalFuture =
                CompletableFuture.allOf(addBookingFutures.stream().toArray(CompletableFuture[]::new));

        CompletableFuture<List<BookingDto>> addBookingCompletableFuture = generalFuture.thenApply(
                g -> addBookingFutures.stream().map(f -> f.join()).collect(Collectors.toList()));

        bookingDtos = addBookingCompletableFuture.join(); //All added booking

        //Simulate concurrent 'edit' booking
        List<CompletableFuture<UpdateResultDto>> editBookingFutures = bookingDtos.stream()
                .filter(bookingDto -> bookingDto.getConfirmCode() != null).parallel()
                .map(bookingDto ->
                        CompletableFuture.supplyAsync(() -> this.editBooking(bookingDto), executorService)
                                .exceptionally(this::handleEditBookingException))
                .collect(Collectors.toList());

        CompletableFuture<Void> generalFutureForEdit =
                CompletableFuture.allOf(editBookingFutures.stream().toArray(CompletableFuture[]::new));

        CompletableFuture<List<UpdateResultDto>> editBookingCompletableFuture = generalFutureForEdit.thenApply(
                g -> editBookingFutures.stream().map(f -> f.join()).collect(Collectors.toList()));

        List<UpdateResultDto> updateResultDtos = editBookingCompletableFuture.join();
    }

    private BookingDto handleAddBookingException(Throwable e) {
        if (e instanceof CompletionException
                && (e.getCause() instanceof DateNotAvailabeException || e.getCause() instanceof NoPickupDateException)) {
            //Expected exception:
            //As between user picks up the date and submits the booking, other user may also reserve the same/overlap date.
            logger.warn(e.getCause().getMessage());
        } else {
            throw new RuntimeException(e);
        }
        return new BookingDto();
    }

    private UpdateResultDto handleEditBookingException(Throwable e) {
        if (e instanceof CompletionException
                && (e.getCause() instanceof DateNotAvailabeException || e.getCause() instanceof NoPickupDateException)) {
            //Expected exception:
            //As between user picks up the date and submits the booking, other user may also reserve the same/overlap date.
            logger.warn(e.getCause().getMessage());
        } else {
            throw new RuntimeException(e);
        }
        return new UpdateResultDto();
    }

    private UpdateResultDto editBooking(BookingDto bookingDto) {
        LocalDate[] avDates = pickUpdateDates(bookingDto.getConfirmCode());
        if (avDates != null && avDates.length > 0) {
            bookingDto.setBookingDates(Arrays.asList(avDates));
            return cacheableBookingService.updateBooking(bookingDto);
        }
        return new UpdateResultDto();

    }

    private BookingDto makeBooking() {
        LocalDate[] localDates = pickUpdateDates(null);
        if (localDates == null || localDates.length == 0) {
            new BookingDto();
        }

        BookingDto bookingDto = createBookingDto(localDates);
        BookingDto rtnBookingDto = cacheableBookingService.addBooking(bookingDto);
        return rtnBookingDto;

    }

    private LocalDate[] pickUpdateDates( String confirmCode ) {
        LocalDate today = LocalDate.now();
        List<LocalDate> availableDates = null;
        if (null == confirmCode) {
            availableDates = cacheableBookingService
                    .getAvailableDates(today.plusDays(1), today.plusDays(CacheableBookingServiceImpl.MAX_DAYS));
        } else {
            availableDates = cacheableBookingService
                    .getAvailableDatesForCurrentBooking(today.plusDays(1), today.plusDays(CacheableBookingServiceImpl.MAX_DAYS), confirmCode);
        }

        int dateSize = availableDates.size();
        if (dateSize == 0) {
            return new LocalDate[0];
        }
        Random random = new Random();
        int numOfPickupDates = random.nextInt(Math.min(dateSize, 3)) + 1; // 1< numOfPickupDates <3
        List<LocalDate> localDates = new ArrayList<>();

        availableDates.sort((o1, o2) -> o1.compareTo(o2));
        int startIndex = random.nextInt(dateSize);
        for (int i = 0; i<numOfPickupDates; i++) {
            if (i == 0) {
                localDates.add(availableDates.get(startIndex));
            } else {
                //Only find the consecutive dates
                int nextIndex = startIndex +i;
                if ( nextIndex < dateSize && localDates.get(0).plusDays(i).isEqual(availableDates.get(nextIndex ))) {
                    localDates.add(availableDates.get(startIndex +i ));
                }
            }
        }
        logger.info("Pickup the dates: " + localDates);
        return localDates.toArray(new LocalDate[0]);
    }

    private BookingDto createBookingDto(LocalDate... localDates) {
        BookingDto bookingDto = new BookingDto();
        bookingDto.setEmail("a@a.com");
        bookingDto.setFirstName("MyFirstName");
        bookingDto.setLastName("MyLastName");
        bookingDto.setBookingDates(Arrays.asList(localDates));

//        String confirmCode = RandomStringUtils.random(10, true, true);
//        bookingDto.setConfirmCode(confirmCode); //random is expensive, gen the confirm code before transaction.

        return bookingDto;
    }

}
