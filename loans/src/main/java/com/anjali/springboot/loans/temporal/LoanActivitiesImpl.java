package com.anjali.springboot.loans.temporal;

import com.anjali.springboot.loans.constants.LoansConstants;
import com.anjali.springboot.loans.entity.Loans;
import com.anjali.springboot.loans.repository.LoansRepository;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LoanActivitiesImpl implements LoanActivities {
  @Autowired private LoansRepository loansRepository;

  @Override
  public boolean evaluateCreditRisk(String mobileNumber, int amount) {
    return amount <= 500000; // Rechazar exageraciones lógicas
  }

  @Override
  public String createLoanApplication(String mobileNumber, int amount) {
    Loans newLoan = new Loans();
    String randomLoanNumber = Long.toString(100000000000L + new Random().nextInt(900000000));
    newLoan.setLoanNumber(randomLoanNumber);
    newLoan.setMobileNumber(mobileNumber);
    newLoan.setLoanType(LoansConstants.HOME_LOAN);
    newLoan.setTotalLoan(amount);
    newLoan.setAmountPaid(0);
    newLoan.setOutstandingAmount(amount);
    newLoan.setStatus("PENDING_VALIDATION");
    loansRepository.save(newLoan);
    return randomLoanNumber;
  }

  @Override
  public void updateLoanStatus(String loanNumber, String status) {
    Loans loan = loansRepository.findByLoanNumber(loanNumber).orElseThrow();
    loan.setStatus(status);
    loansRepository.save(loan);
  }
}
