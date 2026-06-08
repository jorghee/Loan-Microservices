package com.anjali.springboot.loans.temporal;

import com.anjali.springboot.loans.dto.ApplyLoanRequestDto;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface LoanOriginationWorkflow {
  @WorkflowMethod
  String applyForLoan(ApplyLoanRequestDto request);

  @SignalMethod
  void approveManual();

  @SignalMethod
  void rejectManual();
}
