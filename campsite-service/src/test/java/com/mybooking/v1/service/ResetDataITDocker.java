package com.mybooking.v1.service;

import com.mybooking.campsite.Application;
import com.mybooking.campsite.v1.repository.dao.BookingDateRepository;
import com.mybooking.campsite.v1.repository.dao.BookingRepository;
import com.mybooking.campsite.v1.service.CacheableBookingServiceImpl;
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

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = Application.class)
@AutoConfigureMockMvc
//@TestPropertySource(
//        locations = "classpath:application-integrationtest.yml")
public class ResetDataITDocker {

    private final Logger logger = LoggerFactory.getLogger( this.getClass() );

    @Autowired
    @Qualifier("cacheableBookingServiceImpl")
    private CacheableBookingServiceImpl cacheableBookingService;


    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingDateRepository bookingDateRepository;

    @Test
    public void testResetAllData() {
        bookingDateRepository.deleteAll();
        bookingRepository.deleteAll();
        this.cacheableBookingService.getCachedBookingDates().delete();
    }

}
