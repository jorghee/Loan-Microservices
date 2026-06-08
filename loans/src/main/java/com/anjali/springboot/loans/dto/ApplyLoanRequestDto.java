package com.anjali.springboot.loans.dto;

import lombok.Data;

@Data
public class ApplyLoanRequestDto {
  private String mobileNumber;
  private int totalLoan;
}
