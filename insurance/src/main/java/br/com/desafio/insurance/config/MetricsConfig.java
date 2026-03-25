package br.com.desafio.insurance.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Global Micrometer configuration.
 *
 * <p>Adds common tags to EVERY metric emitted by this service so that
 * Datadog dashboards can filter/group by service, environment and version
 * without having to tag each metric individually.
 *
 * <p>Custom business metrics are registered inline via {@link MeterRegistry}
 * injected directly into each component (Controller, Service, Adapter).
 *
 * <h3>Metrics catalogue</h3>
 * <ul>
 *   <li>{@code insurance_quote_created_total}          – quotes persisted OK</li>
 *   <li>{@code insurance_quote_validation_error_total} – business validation failures</li>
 *   <li>{@code insurance_service_unavailable_total}    – circuit-breaker / 503 rejections</li>
 *   <li>{@code insurance_policy_updated_total}         – quotes promoted to APPROVED</li>
 *   <li>{@code insurance_quote_creation_duration}      – end-to-end POST latency (p50/p95/p99)</li>
 *   <li>{@code insurance_catalog_calls_total}          – outbound catalog service calls</li>
 * </ul>
 */
@Configuration
public class MetricsConfig {

    /**
     * Attaches common tags to every meter so Datadog can group/filter without
     * per-metric tag repetition.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name:insurance-quote-service}") String appName,
            @Value("${otel.resource.attributes.deployment\\.environment:local}") String env,
            @Value("${otel.resource.attributes.service\\.version:0.0.1}") String version) {

        return registry -> registry.config()
                .commonTags(
                        "service", appName,
                        "env",     env,
                        "version", version
                );
    }
}
