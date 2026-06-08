package com.anjali.springboot.accounts.config;

import com.anjali.springboot.accounts.temporal.AccountsActivitiesImpl;
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
  public static final String ACCOUNTS_TASK_QUEUE = "ACCOUNTS_TASK_QUEUE";

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
      WorkflowClient workflowClient, AccountsActivitiesImpl accountsActivities) {
    WorkerFactory factory = WorkerFactory.newInstance(workflowClient);
    Worker worker = factory.newWorker(ACCOUNTS_TASK_QUEUE);
    worker.registerActivitiesImplementations(accountsActivities);
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
