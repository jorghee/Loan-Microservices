package com.anjali.springboot.accounts.temporal;

import com.anjali.springboot.accounts.entity.Accounts;
import com.anjali.springboot.accounts.entity.Customer;
import com.anjali.springboot.accounts.repository.AccountsRepository;
import com.anjali.springboot.accounts.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AccountsActivitiesImpl implements AccountsActivities {
  @Autowired private CustomerRepository customerRepository;
  @Autowired private AccountsRepository accountsRepository;

  @Override
  public boolean verifyCustomerAndAccount(String mobileNumber) {
    Customer customer =
        customerRepository
            .findByMobileNumber(mobileNumber)
            .orElseThrow(() -> new RuntimeException("Customer not found"));
    return accountsRepository.findByCustomerId(customer.getCustomerId()).isPresent();
  }

  @Override
  public boolean disburseFunds(String mobileNumber, int amount) {
    Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow();
    Accounts account = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow();

    // Simulación de error (cuenta congelada)
    if (amount < 0) throw new RuntimeException("Invalid disbursement amount");

    account.setBalance(account.getBalance() + amount);
    accountsRepository.save(account);
    return true;
  }

  @Override
  public void reverseDisbursement(String mobileNumber, int amount) {
    Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow();
    Accounts account = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow();

    account.setBalance(account.getBalance() - amount);
    accountsRepository.save(account);
  }
}
