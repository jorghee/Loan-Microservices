package com.anjali.springboot.loans.controller;

import com.anjali.springboot.loans.constants.LoansConstants;
import com.anjali.springboot.loans.dto.ApplyLoanRequestDto;
import com.anjali.springboot.loans.dto.ErrorResponseDto;
import com.anjali.springboot.loans.dto.LoansContactInfoDto;
import com.anjali.springboot.loans.dto.LoansDto;
import com.anjali.springboot.loans.dto.ResponseDto;
import com.anjali.springboot.loans.service.ILoansService;
import com.anjali.springboot.loans.temporal.LoanOriginationWorkflow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(
    name = "CRUD REST APIs for Loans in Capital First",
    description =
        "CRUD REST APIs in Capital First to CREATE, UPDATE, FETCH AND DELETE loan details")
@RestController
@RequestMapping(
    path = "/api",
    produces = {MediaType.APPLICATION_JSON_VALUE})
@Validated
public class LoansController {
  private static final Logger logger = LoggerFactory.getLogger(LoansController.class);
  @Autowired private ILoansService iLoansService;
  @Autowired private LoansContactInfoDto loansContactInfoDto;
  @Autowired private WorkflowClient workflowClient;

  @Operation(
      summary = "Create Loan REST API",
      description = "REST API to create new loan inside Capital First Bank")
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
    @ApiResponse(
        responseCode = "500",
        description = "HTTP Status Internal Server Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  })
  @PostMapping("/create")
  public ResponseEntity<ResponseDto> createLoan(
      @RequestParam @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
          String mobileNumber) {
    iLoansService.createLoan(mobileNumber);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ResponseDto(LoansConstants.STATUS_201, LoansConstants.MESSAGE_201));
  }

  @Operation(
      summary = "Fetch Loan Details REST API",
      description = "REST API to fetch loan details based on a mobile number")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
    @ApiResponse(
        responseCode = "500",
        description = "HTTP Status Internal Server Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  })
  @GetMapping("/fetch")
  public ResponseEntity<LoansDto> fetchLoanDetails(
      @RequestParam @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
          String mobileNumber,
      @RequestHeader("roadMapLearner-correlation-id") String correlationId) {
    logger.debug("roadMapLearner-correlation-id found {}", correlationId);
    LoansDto loansDto = iLoansService.fetchLoan(mobileNumber);
    return ResponseEntity.status(HttpStatus.OK).body(loansDto);
  }

  @Operation(
      summary = "Update Loan Details REST API",
      description = "REST API to update loan details based on a loan number")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
    @ApiResponse(responseCode = "417", description = "Expectation Failed"),
    @ApiResponse(
        responseCode = "500",
        description = "HTTP Status Internal Server Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  })
  @PutMapping("/update")
  public ResponseEntity<ResponseDto> updateLoanDetails(@Valid @RequestBody LoansDto loansDto) {
    boolean isUpdated = iLoansService.updateLoan(loansDto);
    if (isUpdated) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResponseDto(LoansConstants.STATUS_200, LoansConstants.MESSAGE_200));
    } else {
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
          .body(new ResponseDto(LoansConstants.STATUS_417, LoansConstants.MESSAGE_417_UPDATE));
    }
  }

  @Operation(
      summary = "Delete Loan Details REST API",
      description = "REST API to delete Loan details based on a mobile number")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
    @ApiResponse(responseCode = "417", description = "Expectation Failed"),
    @ApiResponse(
        responseCode = "500",
        description = "HTTP Status Internal Server Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  })
  @DeleteMapping("/delete")
  public ResponseEntity<ResponseDto> deleteLoanDetails(
      @RequestParam @Pattern(regexp = "(^$|[0-9]{10})", message = "Mobile number must be 10 digits")
          String mobileNumber) {
    boolean isDeleted = iLoansService.deleteLoan(mobileNumber);
    if (isDeleted) {
      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResponseDto(LoansConstants.STATUS_200, LoansConstants.MESSAGE_200));
    } else {
      return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
          .body(new ResponseDto(LoansConstants.STATUS_417, LoansConstants.MESSAGE_417_DELETE));
    }
  }

  @Operation(
      summary = "Get Contact Info",
      description = "Contact Info details that can be reached out in case of any issues")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
    @ApiResponse(
        responseCode = "500",
        description = "HTTP Status Internal Server Error",
        content = @Content(schema = @Schema(implementation = ErrorResponseDto.class)))
  })
  @GetMapping("/contact-info")
  public ResponseEntity<LoansContactInfoDto> getContactInfo() {

    return ResponseEntity.status(HttpStatus.OK).body(loansContactInfoDto);
  }

  @PostMapping("/applyDistributed")
  @Operation(
      summary = "Apply Distributed Loan",
      description = "Inicia un Workflow orquestado con Temporal")
  public ResponseEntity<ResponseDto> applyDistributedLoan(
      @RequestBody ApplyLoanRequestDto requestDto) {

    // El ID del workflow evita ejecuciones duplicadas (Idempotencia)
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue("LOANS_TASK_QUEUE")
            .setWorkflowId("LOAN_APPL_" + requestDto.getMobileNumber())
            .build();

    LoanOriginationWorkflow workflow =
        workflowClient.newWorkflowStub(LoanOriginationWorkflow.class, options);

    // Llamada Asíncrona: Iniciamos el workflow y devolvemos 202 Inmediatamente
    WorkflowClient.start(workflow::applyForLoan, requestDto);

    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new ResponseDto("202", "Loan origination workflow started successfully"));
  }

  @PostMapping("/approveManual/{mobileNumber}")
  public ResponseEntity<ResponseDto> approveLoanManual(@PathVariable String mobileNumber) {
    LoanOriginationWorkflow workflow =
        workflowClient.newWorkflowStub(LoanOriginationWorkflow.class, "LOAN_APPL_" + mobileNumber);
    workflow.approveManual(); // Envía el Signal
    return ResponseEntity.ok(new ResponseDto("200", "Loan approved manually"));
  }
}
