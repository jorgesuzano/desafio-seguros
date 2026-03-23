package br.com.desafio.insurance.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração dos clientes Feign
 */
@Configuration
@EnableFeignClients(basePackages = "br.com.desafio.insurance.http.client")
public class FeignClientConfig {
}