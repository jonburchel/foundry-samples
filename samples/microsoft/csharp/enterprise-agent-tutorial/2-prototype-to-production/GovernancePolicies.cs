using System;
using System.Collections.Generic;
using System.IO;
using System.Text.Json;
using System.Threading.Tasks;

namespace Microsoft.Azure.Samples.PrototypeToProduction
{
    /// <summary>
    /// Governance Policies module for fleet-wide policy enforcement.
    /// This module implements:
    /// - Content safety policies
    /// - Data protection and PII handling
    /// - Rate limiting and quota management
    /// - Audit logging and compliance
    /// - Regulatory compliance frameworks
    /// </summary>
    public class GovernancePolicies
    {
        /// <summary>
        /// Applies comprehensive governance policies to an agent.
        /// </summary>
        /// <param name="agentName">Name of the agent to apply policies to</param>
        /// <returns>Dictionary containing policy application results</returns>
        public async Task<Dictionary<string, object>> ApplyGovernancePoliciesAsync(string agentName)
        {
            Console.WriteLine("\n🏛️  APPLYING GOVERNANCE POLICIES");
            Console.WriteLine(new string('=', 70));

            // <governance_policy_definition>
            // NOTE: This code is a non-runnable snippet of the larger sample code from which it is taken.
            // Define organization-wide policies
            var governancePolicies = new Dictionary<string, object>
            {
                ["content_safety"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["filters"] = new[] { "hate", "violence", "self_harm", "sexual" },
                    ["action"] = "block_and_log"
                },
                ["data_protection"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["pii_detection"] = true,
                    ["pii_handling"] = "redact",
                    ["sensitive_data_logging"] = false
                },
                ["rate_limiting"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["requests_per_minute"] = 60,
                    ["requests_per_day"] = 5000,
                    ["burst_limit"] = 10
                },
                ["audit_logging"] = new Dictionary<string, object>
                {
                    ["enabled"] = true,
                    ["log_requests"] = true,
                    ["log_responses"] = true,
                    ["log_tokens"] = true,
                    ["retention_days"] = 90
                },
                ["compliance"] = new Dictionary<string, object>
                {
                    ["gdpr_compliant"] = true,
                    ["data_residency"] = "US",
                    ["hipaa_compliant"] = false,
                    ["soc2_compliant"] = true
                }
            };
            // </governance_policy_definition>

            Console.WriteLine("📋 Governance Policies:");
            foreach (var policy in governancePolicies)
            {
                var policyConfig = (Dictionary<string, object>)policy.Value;
                var enabled = policyConfig.ContainsKey("enabled") && (bool)policyConfig["enabled"];
                var status = enabled ? "✅ ACTIVE" : "⚠️  INACTIVE";
                Console.WriteLine($"   {status} {policy.Key.Replace("_", " ").ToUpper()}");
            }

            var policyApplication = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["agent_name"] = agentName,
                ["policies_applied"] = governancePolicies,
                ["enforcement_level"] = "strict",
                ["compliance_status"] = "compliant"
            };

            await File.WriteAllTextAsync("governance_policies.json",
                JsonSerializer.Serialize(policyApplication, new JsonSerializerOptions { WriteIndented = true }));

            Console.WriteLine($"\n✅ Governance policies applied successfully");
            Console.WriteLine($"   📄 Configuration saved to: governance_policies.json");
            Console.WriteLine($"   🔒 Enforcement Level: {policyApplication["enforcement_level"]}");

            return policyApplication;
        }

        /// <summary>
        /// Validates that an agent configuration meets governance requirements.
        /// </summary>
        /// <param name="agentConfig">Agent configuration to validate</param>
        /// <returns>Validation results with any violations</returns>
        public Dictionary<string, object> ValidateAgentCompliance(Dictionary<string, object> agentConfig)
        {
            var violations = new List<string>();
            var warnings = new List<string>();

            // Check content safety
            if (!agentConfig.ContainsKey("content_safety_enabled") || 
                !(bool)agentConfig["content_safety_enabled"])
            {
                violations.Add("Content safety must be enabled");
            }

            // Check data protection
            if (!agentConfig.ContainsKey("pii_detection") || 
                !(bool)agentConfig["pii_detection"])
            {
                warnings.Add("PII detection should be enabled for data protection");
            }

            // Check audit logging
            if (!agentConfig.ContainsKey("audit_logging") || 
                !(bool)agentConfig["audit_logging"])
            {
                violations.Add("Audit logging is required for compliance");
            }

            var isCompliant = violations.Count == 0;

            var validation = new Dictionary<string, object>
            {
                ["is_compliant"] = isCompliant,
                ["violations"] = violations,
                ["warnings"] = warnings,
                ["timestamp"] = DateTime.UtcNow.ToString("O")
            };

            return validation;
        }

        /// <summary>
        /// Generates a compliance report for an agent or fleet.
        /// </summary>
        /// <param name="agentName">Name of the agent</param>
        /// <param name="policyResults">Policy application results</param>
        /// <returns>Formatted compliance report</returns>
        public string GenerateComplianceReport(string agentName, Dictionary<string, object> policyResults)
        {
            var report = new System.Text.StringBuilder();
            report.AppendLine("╔════════════════════════════════════════════════════════════════════╗");
            report.AppendLine("║           GOVERNANCE COMPLIANCE REPORT                             ║");
            report.AppendLine("╚════════════════════════════════════════════════════════════════════╝");
            report.AppendLine();

            report.AppendLine($"Agent: {agentName}");
            report.AppendLine($"Report Date: {policyResults["timestamp"]}");
            report.AppendLine($"Compliance Status: {policyResults["compliance_status"]}");
            report.AppendLine($"Enforcement Level: {policyResults["enforcement_level"]}");
            report.AppendLine();

            var policies = (Dictionary<string, object>)policyResults["policies_applied"];
            report.AppendLine("Policy Status:");
            report.AppendLine(new string('-', 70));

            foreach (var policy in policies)
            {
                var policyName = policy.Key.Replace("_", " ").ToUpper();
                var policyConfig = (Dictionary<string, object>)policy.Value;
                var enabled = policyConfig.ContainsKey("enabled") && (bool)policyConfig["enabled"];
                
                var symbol = enabled ? "✅" : "❌";
                report.AppendLine($"{symbol} {policyName}");

                // Show key settings for each policy
                foreach (var setting in policyConfig)
                {
                    if (setting.Key != "enabled")
                    {
                        report.AppendLine($"   - {setting.Key}: {setting.Value}");
                    }
                }
            }

            report.AppendLine();
            report.AppendLine("Compliance Frameworks:");
            report.AppendLine(new string('-', 70));

            var compliance = (Dictionary<string, object>)policies["compliance"];
            foreach (var framework in compliance)
            {
                if (framework.Value is bool boolValue)
                {
                    var symbol = boolValue ? "✅" : "❌";
                    report.AppendLine($"{symbol} {framework.Key.ToUpper()}");
                }
                else
                {
                    report.AppendLine($"   {framework.Key}: {framework.Value}");
                }
            }

            return report.ToString();
        }

        /// <summary>
        /// Checks if rate limits are being enforced correctly.
        /// </summary>
        /// <param name="requestsInLastMinute">Number of requests in the last minute</param>
        /// <param name="requestsInLastDay">Number of requests in the last day</param>
        /// <param name="policies">Governance policies configuration</param>
        /// <returns>Rate limit status</returns>
        public Dictionary<string, object> CheckRateLimits(
            int requestsInLastMinute, 
            int requestsInLastDay,
            Dictionary<string, object> policies)
        {
            var rateLimitConfig = (Dictionary<string, object>)policies["rate_limiting"];
            var maxPerMinute = (int)(long)rateLimitConfig["requests_per_minute"];
            var maxPerDay = (int)(long)rateLimitConfig["requests_per_day"];

            var withinMinuteLimit = requestsInLastMinute <= maxPerMinute;
            var withinDayLimit = requestsInLastDay <= maxPerDay;

            var status = new Dictionary<string, object>
            {
                ["within_limits"] = withinMinuteLimit && withinDayLimit,
                ["requests_last_minute"] = requestsInLastMinute,
                ["limit_per_minute"] = maxPerMinute,
                ["minute_limit_ok"] = withinMinuteLimit,
                ["requests_last_day"] = requestsInLastDay,
                ["limit_per_day"] = maxPerDay,
                ["day_limit_ok"] = withinDayLimit,
                ["timestamp"] = DateTime.UtcNow.ToString("O")
            };

            return status;
        }

        /// <summary>
        /// Creates an audit log entry for tracking agent operations.
        /// </summary>
        /// <param name="agentName">Name of the agent</param>
        /// <param name="operation">Operation performed</param>
        /// <param name="details">Additional operation details</param>
        /// <returns>Audit log entry</returns>
        public Dictionary<string, object> CreateAuditLogEntry(
            string agentName,
            string operation,
            Dictionary<string, object> details)
        {
            var auditEntry = new Dictionary<string, object>
            {
                ["timestamp"] = DateTime.UtcNow.ToString("O"),
                ["agent_name"] = agentName,
                ["operation"] = operation,
                ["details"] = details,
                ["log_level"] = "INFO",
                ["correlation_id"] = Guid.NewGuid().ToString()
            };

            return auditEntry;
        }

        /// <summary>
        /// Enforces data residency requirements based on governance policies.
        /// </summary>
        /// <param name="requestRegion">Region where the request originated</param>
        /// <param name="policies">Governance policies</param>
        /// <returns>Data residency validation result</returns>
        public Dictionary<string, object> ValidateDataResidency(
            string requestRegion,
            Dictionary<string, object> policies)
        {
            var compliance = (Dictionary<string, object>)policies["compliance"];
            var allowedResidency = (string)compliance["data_residency"];

            var isCompliant = requestRegion.Equals(allowedResidency, StringComparison.OrdinalIgnoreCase);

            var validation = new Dictionary<string, object>
            {
                ["is_compliant"] = isCompliant,
                ["request_region"] = requestRegion,
                ["allowed_residency"] = allowedResidency,
                ["timestamp"] = DateTime.UtcNow.ToString("O")
            };

            if (!isCompliant)
            {
                validation["violation"] = $"Request from {requestRegion} violates data residency policy requiring {allowedResidency}";
            }

            return validation;
        }
    }
}
