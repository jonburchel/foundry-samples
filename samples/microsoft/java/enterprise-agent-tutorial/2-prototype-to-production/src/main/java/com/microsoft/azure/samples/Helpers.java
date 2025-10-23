// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.samples;

/**
 * Helper utilities for the Tutorial 2 application.
 * Provides common formatting and utility functions.
 */
public class Helpers {
    
    /**
     * Print a formatted section header.
     * 
     * @param title The section title
     */
    public static void printSectionHeader(String title) {
        System.out.println("\n" + title);
        System.out.println("=".repeat(70));
    }
    
    /**
     * Print a formatted subsection.
     * 
     * @param icon The icon to display
     * @param message The message text
     */
    public static void printSubsection(String icon, String message) {
        System.out.println(icon + " " + message);
    }
    
    /**
     * Print an indented item.
     * 
     * @param message The message to print
     */
    public static void printIndented(String message) {
        System.out.println("   " + message);
    }
    
    /**
     * Print a success message.
     * 
     * @param message The success message
     */
    public static void printSuccess(String message) {
        System.out.println("✅ " + message);
    }
    
    /**
     * Print a warning message.
     * 
     * @param message The warning message
     */
    public static void printWarning(String message) {
        System.out.println("⚠️  " + message);
    }
    
    /**
     * Print an error message.
     * 
     * @param message The error message
     */
    public static void printError(String message) {
        System.err.println("❌ " + message);
    }
    
    /**
     * Print a completion summary.
     */
    public static void printCompletionSummary() {
        System.out.println("\n🎉 TUTORIAL 2 COMPLETE - PRODUCTION READY!");
        System.out.println("=".repeat(70));
        System.out.println("✅ Safety assessment completed");
        System.out.println("✅ Evaluation baseline established");
        System.out.println("✅ Governance policies applied");
        System.out.println("✅ Model configurations compared");
        System.out.println("✅ Fleet monitoring configured");
        System.out.println("✅ CI/CD integration ready");
        System.out.println("✅ Production endpoint configured");
        System.out.println("\n📊 Review generated artifacts:");
        System.out.println("   • safety_assessment.json");
        System.out.println("   • evaluation_dataset.jsonl");
        System.out.println("   • baseline_evaluation.json");
        System.out.println("   • governance_policies.json");
        System.out.println("   • model_comparison.json");
        System.out.println("   • fleet_monitoring.json");
        System.out.println("   • cicd_config.json");
        System.out.println("   • .github-workflows-agent-evaluation.yml");
        System.out.println("   • azure-pipelines.yml");
        System.out.println("   • endpoint_config.json");
        System.out.println("   • logic_app_definition.json");
    }
}
