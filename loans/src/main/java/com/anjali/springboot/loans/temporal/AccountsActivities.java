package com.anjali.springboot.loans.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface AccountsActivities {
  @ActivityMethod
  boolean verifyCustomerAndAccount(String mobileNumber);

  @ActivityMethod
  boolean disburseFunds(String mobileNumber, int amount);

  @ActivityMethod
  void reverseDisbursement(String mobileNumber, int amount);
}
