package com.mybooking.campsite.v1.repository.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import java.time.LocalDate;

@Entity
@Table(name = "BookingDate",
        uniqueConstraints = { @UniqueConstraint(name = "UC_BookingDate_RevDate", columnNames={"Rev_Date"}) })
public class BookingDate {

    @Id
    @GeneratedValue( strategy = GenerationType.IDENTITY )
    @Column( name = "Id" )
    private Long id;

    @Column(name = "Version")
    @Version
    private Integer version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "Booking_Id", foreignKey = @ForeignKey( name = "FK_BookingDate_Booking_Id" ))
    private Booking booking;

    @Column(name="Rev_Date",  columnDefinition = "DATE", nullable=false)
    private LocalDate revDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDate getRevDate() {
        return revDate;
    }

    public void setRevDate(LocalDate revDate) {
        this.revDate = revDate;
    }
}
