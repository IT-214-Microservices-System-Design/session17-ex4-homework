package com.grabfood.flashsale.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cấu hình CacheManager sử dụng Caffeine Cache hỗ trợ hoàn hảo cơ chế đồng bộ khóa (sync = true).
     * Khi có 50 request đồng thời truy cập vào cùng 1 key bị Cache Miss (ví dụ khi vừa expire TTL),
     * cơ chế khóa của Caffeine kết hợp thuộc tính sync = true sẽ đảm bảo chỉ DUY NHẤT 1 luồng
     * được phép truy vấn Database, các luồng còn lại sẽ bị block chờ và nhận kết quả từ cache.
     */
    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("flash-sale");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(10_000)
                .recordStats());
        return cacheManager;
    }
}
