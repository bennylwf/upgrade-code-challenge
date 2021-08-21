create table Booking (Id bigint not null auto_increment, Confirm_Code varchar(10) not null, Email varchar(254) not null, First_Name varchar(30) not null, Last_Name varchar(30) not null, Version integer, primary key (Id)) engine=InnoDB
create table BookingDate (Id bigint not null auto_increment, Rev_Date DATE not null, Version integer, Booking_Id bigint, primary key (Id)) engine=InnoDB
alter table Booking add constraint UC_Booking_ConfirmCode unique (Confirm_Code)
alter table BookingDate add constraint UC_BookingDate_RevDate unique (Rev_Date)
alter table BookingDate add constraint FK_BookingDate_Booking_Id foreign key (Booking_Id) references Booking (Id)
create table Booking (Id bigint not null auto_increment, Confirm_Code varchar(10) not null, Email varchar(254) not null, First_Name varchar(30) not null, Last_Name varchar(30) not null, Version integer, primary key (Id)) engine=InnoDB
create table BookingDate (Id bigint not null auto_increment, Rev_Date DATE not null, Version integer, Booking_Id bigint, primary key (Id)) engine=InnoDB
alter table Booking add constraint UC_Booking_ConfirmCode unique (Confirm_Code)
alter table BookingDate add constraint UC_BookingDate_RevDate unique (Rev_Date)
alter table BookingDate add constraint FK_BookingDate_Booking_Id foreign key (Booking_Id) references Booking (Id)
