package com.mybooking.campsite.v1.shared.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DeleteResultDto {
    private List<LocalDate> deletedDates;
}
