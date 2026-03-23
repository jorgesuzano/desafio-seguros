package br.com.desafio.insurance.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração de cache para produtos e ofertas
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Define dois caches: um para produtos e outro para ofertas
     * Você pode substituir por Redis ou outro provedor em produção
     *
     * @return CacheManager configurado
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("products", "offers");
    }
}