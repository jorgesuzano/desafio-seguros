package br.com.desafio.insurance.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "br.com.desafio.insurance.adapter.out.catalog")
public class FeignClientConfig {
}