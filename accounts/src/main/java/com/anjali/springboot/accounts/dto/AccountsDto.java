package com.anjali.springboot.accounts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Schema(name = "Accounts", description = "Schema to hold Account information")
@Data
public class AccountsDto {
  @Schema(description = "Account number")
  @NotEmpty(message = "Account Number can not be empty")
  @Pattern(regexp = "(^$|[0-9]{10})", message = "Account Number must be 10 digits")
  private Long accountNumber;

  @Schema(description = "Account type", example = "Savings")
  @NotEmpty(message = "Account Type can not be empty")
  private String accountType;

  @Schema(description = "First Capital branch Address")
  @NotEmpty(message = "Branch Address can not be empty")
  private String branchAddress;

  @Schema(description = "Account balance")
  private int balance;
}
