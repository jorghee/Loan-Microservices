package com.anjali.springboot.loans.temporal;

import com.anjali.springboot.loans.dto.ApplyLoanRequestDto;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class LoanOriginationWorkflowImpl implements LoanOriginationWorkflow {

  private boolean isApprovedManual = false;
  private boolean isRejectedManual = false;

  // Conexión a la cola local (Loans)
  private final LoanActivities loanActivities =
      Workflow.newActivityStub(
          LoanActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  // Conexión a la cola remota (Accounts) con reintentos configurados
  private final AccountsActivities accountsActivities =
      Workflow.newActivityStub(
          AccountsActivities.class,
          ActivityOptions.newBuilder()
              .setTaskQueue("ACCOUNTS_TASK_QUEUE")
              .setStartToCloseTimeout(Duration.ofSeconds(10))
              .setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build())
              .build());

  @Override
  public String applyForLoan(ApplyLoanRequestDto request) {
    Saga saga = new Saga(new Saga.Options.Builder().setParallelCompensation(false).build());
    String loanNumber = null;

    try {
      // Verificación en otro MS (Síncrono/Bloqueante en Temporal)
      boolean isValidCustomer =
          accountsActivities.verifyCustomerAndAccount(request.getMobileNumber());
      if (!isValidCustomer) return "REJECTED_CUSTOMER_NOT_FOUND";

      // Crear solicitud inicial
      loanNumber =
          loanActivities.createLoanApplication(request.getMobileNumber(), request.getTotalLoan());

      // Evaluar Riesgo
      loanActivities.updateLoanStatus(loanNumber, "UNDER_REVIEW");
      boolean isRiskAcceptable =
          loanActivities.evaluateCreditRisk(request.getMobileNumber(), request.getTotalLoan());
      if (!isRiskAcceptable) {
        loanActivities.updateLoanStatus(loanNumber, "REJECTED_RISK");
        return "REJECTED_RISK";
      }

      // Aprobación Manual Condicional (Pausa el workflow sin gastar CPU)
      if (request.getTotalLoan() > 50000) {
        loanActivities.updateLoanStatus(loanNumber, "PENDING_APPROVAL_MANUAL");
        Workflow.await(() -> isApprovedManual || isRejectedManual);
        if (isRejectedManual) {
          loanActivities.updateLoanStatus(loanNumber, "REJECTED_MANUAL");
          return "REJECTED_MANUAL";
        }
      }

      // Inicia Desembolso (Zona de SAGA)
      loanActivities.updateLoanStatus(loanNumber, "APPROVED_PENDING_DISBURSEMENT");

      // Añadimos compensación preventiva (Si el desembolso falla, se marcará error)
      saga.addCompensation(loanActivities::updateLoanStatus, loanNumber, "CANCELLED_SYSTEM_ERROR");

      // Ejecutar Desembolso en Accounts
      accountsActivities.disburseFunds(request.getMobileNumber(), request.getTotalLoan());

      // Añadir compensación para revertir desembolso si fallara algo después
      saga.addCompensation(
          accountsActivities::reverseDisbursement,
          request.getMobileNumber(),
          request.getTotalLoan());

      // Éxito Final
      loanActivities.updateLoanStatus(loanNumber, "ACTIVE");
      return "SUCCESS_LOAN_ACTIVE";

    } catch (Exception e) {
      // Si cualquier paso crashea (Accounts no levanta o rechaza el desembolso), compensamos.
      saga.compensate();
      throw Workflow.wrap(e); // Deja constancia del error en el historial
    }
  }

  @Override
  public void approveManual() {
    this.isApprovedManual = true;
  }

  @Override
  public void rejectManual() {
    this.isRejectedManual = true;
  }
}
