package com.anjali.springboot.loans.mapper;

import com.anjali.springboot.loans.dto.LoansDto;
import com.anjali.springboot.loans.entity.Loans;

public class LoanMapper {
  public static LoansDto mapToLoansDto(Loans loans, LoansDto loansDto) {
    loansDto.setLoanNumber(loans.getLoanNumber());
    loansDto.setLoanType(loans.getLoanType());
    loansDto.setMobileNumber(loans.getMobileNumber());
    loansDto.setTotalLoan(loans.getTotalLoan());
    loansDto.setAmountPaid(loans.getAmountPaid());
    loansDto.setOutstandingAmount(loans.getOutstandingAmount());
    loansDto.setStatus(loans.getStatus());
    return loansDto;
  }

  public static Loans mapToLoans(LoansDto loansDto, Loans loans) {
    loans.setLoanNumber(loansDto.getLoanNumber());
    loans.setLoanType(loansDto.getLoanType());
    loans.setMobileNumber(loansDto.getMobileNumber());
    loans.setTotalLoan(loansDto.getTotalLoan());
    loans.setAmountPaid(loansDto.getAmountPaid());
    loans.setOutstandingAmount(loansDto.getOutstandingAmount());
    loans.setStatus(loansDto.getStatus());
    return loans;
  }
}
