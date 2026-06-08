// Archivo: loans/src/main/java/com/anjali/springboot/loans/temporal/LoanActivities.java
package com.anjali.springboot.loans.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface LoanActivities {
  @ActivityMethod
  boolean evaluateCreditRisk(String mobileNumber, int amount);

  @ActivityMethod
  String createLoanApplication(String mobileNumber, int amount);

  @ActivityMethod
  void updateLoanStatus(String loanNumber, String status);
}
