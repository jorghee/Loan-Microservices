package com.anjali.springboot.accounts.mapper;

import com.anjali.springboot.accounts.dto.AccountsDto;
import com.anjali.springboot.accounts.entity.Accounts;

public class AccountsMapper {
  public static AccountsDto mapToAccountsDto(Accounts accounts, AccountsDto accountsDto) {
    accountsDto.setAccountNumber(accounts.getAccountNumber());
    accountsDto.setAccountType(accounts.getAccountType());
    accountsDto.setBranchAddress(accounts.getBranchAddress());
    accountsDto.setBalance(accounts.getBalance());
    return accountsDto;
  }

  public static Accounts mapToAccounts(AccountsDto accountsDto, Accounts accounts) {
    accounts.setAccountNumber(accountsDto.getAccountNumber());
    accounts.setAccountType(accountsDto.getAccountType());
    accounts.setBranchAddress(accountsDto.getBranchAddress());
    accounts.setBalance(accountsDto.getBalance());
    return accounts;
  }
}
