package com.mybooking.campsite.v1.repository.domain;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.util.List;

@Entity
@Table(name = "Booking",
        uniqueConstraints = { @UniqueConstraint(name = "UC_Booking_ConfirmCode", columnNames={"Confirm_Code"}) })
public class Booking {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    @Column( name = "Id" )
    private Long id;

    @Column(name = "Version")
    @Version
    private Integer version;

    @Column( name = "Email", nullable = false, length = 254 )
    private String email;

    @Column( name = "First_Name", nullable = false, length = 30 )
    private String firstName;

    @Column( name = "Last_Name",  nullable = false, length = 30 )
    private String lastName;

    @Column( name = "Confirm_Code",  nullable = false, length = 10 )
    private String confirmCode;

    @OneToMany(
            mappedBy = "booking",
            cascade = CascadeType.ALL
    )
    private List<BookingDate> bookingDates;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getConfirmCode() {
        return confirmCode;
    }

    public void setConfirmCode(String confirmCode) {
        this.confirmCode = confirmCode;
    }

    public List<BookingDate> getBookingDates() {
        return bookingDates;
    }

    public void setBookingDates(List<BookingDate> bookingDates) {
        this.bookingDates = bookingDates;
    }
}