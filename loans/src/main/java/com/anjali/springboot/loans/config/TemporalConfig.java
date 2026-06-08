package com.anjali.springboot.loans.config;

import com.anjali.springboot.loans.temporal.LoanActivitiesImpl;
import com.anjali.springboot.loans.temporal.LoanOriginationWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {
  public static final String LOANS_TASK_QUEUE = "LOANS_TASK_QUEUE";

  private WorkflowServiceStubs serviceStubs;

  @Bean
  public WorkflowServiceStubs workflowServiceStubs() {
    this.serviceStubs = WorkflowServiceStubs.newLocalServiceStubs();
    return this.serviceStubs;
  }

  @Bean
  public WorkflowClient workflowClient(WorkflowServiceStubs serviceStubs) {
    return WorkflowClient.newInstance(serviceStubs);
  }

  @Bean
  public WorkerFactory workerFactory(
      WorkflowClient workflowClient, LoanActivitiesImpl loanActivities) {
    WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
    Worker worker = factory.newWorker(LOANS_TASK_QUEUE);

    // Registro del Workflow
    worker.registerWorkflowImplementationTypes(LoanOriginationWorkflowImpl.class);
    worker.registerActivitiesImplementations(loanActivities);

    factory.start();
    return factory;
  }

  @PreDestroy
  public void shutdown() throws InterruptedException {
    if (serviceStubs != null) {
      serviceStubs.shutdown();
      serviceStubs.awaitTermination(10, TimeUnit.SECONDS);
    }
  }
}
