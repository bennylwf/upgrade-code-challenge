# Campsite Booking System
## Design
This campsite booking is a read heavy system. The data are stored in DB.
It has two tables:
- Booking: Stores the user email, first name, last name, confirmation code 
  (We can normalize the user info in a 'User' table in future enhancement.)
- BookingDate: Stores the reservation dates for this booking

I use redis as the cache server. It caches
all the dates that have been reserved in 30 days. Redis cache is thread safe as Redis is implemented with single thread.
It not only serves as cache but also provides the distributed lock to synchronize the data access. 
It helps to ensure cache data are synchronized across multiple micro service instances if we do want to scale this campsite 
booking system horizontally. Without that, we will have much more DB calls and DB lock. 
DB locks are much more expensive than the redis lock.


## Maven Modules 
 - campsite-spec
   - This module defines OpenApi Spec : CampsiteApi-v1.yaml. It provides two sets of APIs:
     - Booking APIs:
       It can add, update and delete the booking.
     - AvailableDate APIs:
       It provides the available dates within a date range (max 30 future days) for user to pick up.
   - The maven build will generate the server/client code based on the spec.
 
 - campsite-service
   - This module implement the server site logic. The service class - CacheableBookingServiceImpl
    has the most business logic and handles the cache/DB access.
     
## Test Cases
  - CachealbeBookingServiceIT.java
    - It uses the H2 DB and a embeded Redis server. The test case 'testConcurrent_Add_Edit' 
      will simulate the concurrent reservation requests. It picks up the booking dates randomly 
      and makes the reservation, then it will update those reservations/bookings by picking up or releasing 
      some dates back to the pool. Please note that some of add/update booking calls may have 
      DateNotAvailabeException or NoPickupDateException. You will see some warning log. This is to simulate the real 
      world situation: the user selects the date first, then makes the reservation requests. But between that time, 
      those dates may be booked by others.
      - To run it:
        ```
        mvn clean install
        ```
        
  - CachealbeBookingServiceITDocker.java
    - It connects mysql and redis to run similar test cases as the above CachealbeBookingServiceIT. 
      - The default 'mvn test' won't run this test class, you can manually run it:
        ```
        cd <checkout-root-folder>
        mvn clean install
        dock-compose up
        mvn integration-test -Dit.test=CachealbeBookingServiceITDocker
        ```
      
## Run the App
  - You can use following commands to start the app.
  ```
        cd <checkout-root-folder>
        mvn clean install
        dock-compose up
        mvn spring-boot:run
``` 
  - API usage:
    - You can check the API spec 'CampsiteApi-v1.yaml' to see how to use APIs. Below is an example to get the available dates.
   ```
curl --location --request GET 'http://localhost:8080/campsite-service/v1/availableDate?checkInDate=2021-08-01&checkOutDate=2021-08-15'
```

