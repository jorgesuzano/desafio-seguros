package br.com.desafio.insurance.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de cache Caffeine para produtos e ofertas.
 * O spec (TTL / tamanho máximo) é lido de spring.cache.caffeine.spec no application.yml.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.caffeine.spec:maximumSize=500,expireAfterWrite=10m}")
    private String caffeineSpec;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("products", "offers");
        manager.setCaffeine(Caffeine.from(caffeineSpec));
        return manager;
    }
}