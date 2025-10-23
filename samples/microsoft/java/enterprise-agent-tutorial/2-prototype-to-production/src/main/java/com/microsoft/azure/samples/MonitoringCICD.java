// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Handles fleet monitoring configuration and CI/CD pipeline setup.
 * Configures comprehensive monitoring and automated quality gates.
 */
public class MonitoringCICD {
    
    private final Gson gson;
    
    public MonitoringCICD(Gson gson) {
        this.gson = gson;
    }
    
    /**
     * Setup fleet monitoring with comprehensive metrics and alerts.
     */
    public void setupFleetMonitoring() {
        System.out.println("\n📡 FLEET MONITORING SETUP");
        System.out.println("=".repeat(70));

        // <monitoring_metrics_configuration>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        Map<String, Object> monitoringConfig = new HashMap<>();

        // Performance Metrics
        Map<String, Object> performanceMetrics = new HashMap<>();
        performanceMetrics.put("response_latency_ms", createMetric("gauge", "Average response time", 3000));
        performanceMetrics.put("token_usage_per_request", createMetric("gauge", "Tokens per request", 2000));
        performanceMetrics.put("requests_per_minute", createMetric("counter", "Request rate", 100));
        performanceMetrics.put("error_rate_percent", createMetric("gauge", "Error percentage", 5.0));

        // Quality Metrics
        Map<String, Object> qualityMetrics = new HashMap<>();
        qualityMetrics.put("user_satisfaction_score", createMetric("gauge", "User satisfaction", 4.5));
        qualityMetrics.put("task_completion_rate", createMetric("gauge", "Task completion %", 85.0));
        qualityMetrics.put("hallucination_rate", createMetric("gauge", "Hallucination %", 5.0));
        qualityMetrics.put("safety_violations", createMetric("counter", "Safety violations count", 0));

        // Business Metrics
        Map<String, Object> businessMetrics = new HashMap<>();
        businessMetrics.put("active_users", createMetric("gauge", "Active user count", 500));
        businessMetrics.put("questions_answered", createMetric("counter", "Total questions", 10000));
        businessMetrics.put("cost_per_conversation", createMetric("gauge", "Avg cost per conversation", 0.05));
        businessMetrics.put("uptime_percent", createMetric("gauge", "Service uptime %", 99.9));
        // </monitoring_metrics_configuration>

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("performance", performanceMetrics);
        metrics.put("quality", qualityMetrics);
        metrics.put("business", businessMetrics);

        // Alert Configuration
        List<Map<String, Object>> alerts = Arrays.asList(
                createAlert("high_latency", "response_latency_ms", ">", 5000, "critical"),
                createAlert("high_error_rate", "error_rate_percent", ">", 10.0, "warning"),
                createAlert("low_satisfaction", "user_satisfaction_score", "<", 3.0, "warning"),
                createAlert("safety_violation", "safety_violations", ">", 0, "critical")
        );

        monitoringConfig.put("configuration_date", Instant.now().toString());
        monitoringConfig.put("metrics", metrics);
        monitoringConfig.put("alerts", alerts);
        monitoringConfig.put("dashboard_url", "https://portal.azure.com/your-dashboard");
        monitoringConfig.put("log_analytics_workspace", "your-workspace-id");

        saveToFile(monitoringConfig, "fleet_monitoring.json");

        System.out.println("✅ Fleet monitoring configured successfully");
        System.out.println("   Performance metrics: " + performanceMetrics.size());
        System.out.println("   Quality metrics: " + qualityMetrics.size());
        System.out.println("   Business metrics: " + businessMetrics.size());
        System.out.println("   Alert rules: " + alerts.size());
        System.out.println("   Configuration saved: fleet_monitoring.json");
    }
    
    /**
     * Create CI/CD evaluation configuration with automated quality gates.
     */
    public void createCiCdEvaluationConfig() {
        System.out.println("\n🔧 CI/CD OFFLINE EVALUATION SETUP");
        System.out.println("=".repeat(70));

        // <cicd_quality_gates>
        // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
        List<Map<String, Object>> stages = Arrays.asList(
                createQualityGate("safety_assessment", "Safety score must be >80%", 80.0, "block_on_failure"),
                createQualityGate("baseline_evaluation", "Pass rate must be >70%", 70.0, "block_on_failure"),
                createQualityGate("regression_testing", "Regression must be <5%", 5.0, "warn_on_failure"),
                createQualityGate("performance_testing", "Avg latency must be <3000ms", 3000, "warn_on_failure")
        );
        // </cicd_quality_gates>

        Map<String, Object> cicdConfig = new HashMap<>();
        cicdConfig.put("configuration_date", Instant.now().toString());
        cicdConfig.put("pipeline_version", "1.0");
        cicdConfig.put("stages", stages);
        cicdConfig.put("evaluation_dataset", "evaluation_dataset.jsonl");
        cicdConfig.put("artifacts_to_publish", Arrays.asList(
                "safety_assessment.json",
                "baseline_evaluation.json",
                "model_comparison.json"
        ));

        saveToFile(cicdConfig, "cicd_config.json");

        // Generate GitHub Actions workflow
        String githubWorkflow = """
name: Agent Evaluation Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  evaluate:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        java-version: '11'
        distribution: 'temurin'
    
    - name: Azure Login
      uses: azure/login@v1
      with:
        creds: ${{ secrets.AZURE_CREDENTIALS }}
    
    - name: Run Safety Assessment
      run: mvn exec:java -Dexec.args="--safety-only"
    
    - name: Run Baseline Evaluation
      run: mvn exec:java -Dexec.args="--eval-only"
    
    - name: Check Quality Gates
      run: |
        python scripts/check_quality_gates.py
    
    - name: Publish Results
      uses: actions/upload-artifact@v3
      with:
        name: evaluation-results
        path: |
          safety_assessment.json
          baseline_evaluation.json
          model_comparison.json
""";

        saveToFile(githubWorkflow, ".github-workflows-agent-evaluation.yml");

        // Generate Azure DevOps pipeline
        String azureDevOpsPipeline = """
trigger:
  branches:
    include:
    - main

pool:
  vmImage: 'ubuntu-latest'

variables:
  MAVEN_CACHE_FOLDER: $(Pipeline.Workspace)/.m2/repository

steps:
- task: Maven@3
  inputs:
    mavenPomFile: 'pom.xml'
    goals: 'clean install'
    options: '-DskipTests'
    publishJUnitResults: false

- task: AzureCLI@2
  displayName: 'Run Safety Assessment'
  inputs:
    azureSubscription: '$(AzureServiceConnection)'
    scriptType: 'bash'
    scriptLocation: 'inlineScript'
    inlineScript: |
      mvn exec:java -Dexec.args="--safety-only"

- task: AzureCLI@2
  displayName: 'Run Baseline Evaluation'
  inputs:
    azureSubscription: '$(AzureServiceConnection)'
    scriptType: 'bash'
    scriptLocation: 'inlineScript'
    inlineScript: |
      mvn exec:java -Dexec.args="--eval-only"

- task: PythonScript@0
  displayName: 'Check Quality Gates'
  inputs:
    scriptSource: 'filePath'
    scriptPath: 'scripts/check_quality_gates.py'

- task: PublishBuildArtifacts@1
  displayName: 'Publish Evaluation Results'
  inputs:
    PathtoPublish: '$(Build.SourcesDirectory)'
    ArtifactName: 'evaluation-results'
    publishLocation: 'Container'
  condition: always()
""";

        saveToFile(azureDevOpsPipeline, "azure-pipelines.yml");

        System.out.println("✅ CI/CD evaluation configured successfully");
        System.out.println("   Quality gate stages: " + stages.size());
        System.out.println("   Configuration saved: cicd_config.json");
        System.out.println("   GitHub workflow: .github-workflows-agent-evaluation.yml");
        System.out.println("   Azure DevOps pipeline: azure-pipelines.yml");
    }
    
    private Map<String, Object> createMetric(String type, String description, Object threshold) {
        Map<String, Object> metric = new HashMap<>();
        metric.put("type", type);
        metric.put("description", description);
        metric.put("threshold", threshold);
        return metric;
    }
    
    private Map<String, Object> createAlert(String name, String metric, String operator, 
                                            Object threshold, String severity) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("name", name);
        alert.put("metric", metric);
        alert.put("operator", operator);
        alert.put("threshold", threshold);
        alert.put("severity", severity);
        return alert;
    }
    
    private Map<String, Object> createQualityGate(String stage, String description, 
                                                   Object threshold, String action) {
        Map<String, Object> gate = new HashMap<>();
        gate.put("stage", stage);
        gate.put("description", description);
        Map<String, Object> qualityGate = new HashMap<>();
        qualityGate.put("threshold", threshold);
        qualityGate.put("action", action);
        gate.put("quality_gate", qualityGate);
        return gate;
    }
    
    private void saveToFile(Object data, String filename) {
        try {
            if (data instanceof String) {
                FileWriter writer = new FileWriter(filename);
                writer.write((String) data);
                writer.close();
            } else {
                FileWriter writer = new FileWriter(filename);
                gson.toJson(data, writer);
                writer.close();
            }
        } catch (IOException e) {
            System.err.println("Error saving file " + filename + ": " + e.getMessage());
        }
    }
}
