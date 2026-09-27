package com.grabfood.flashsale.service;

import com.grabfood.flashsale.entity.FlashSaleProduct;
import com.grabfood.flashsale.repository.FlashSaleProductRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleProductRepository productRepository;

    @Getter
    private final AtomicInteger dbQueryCount = new AtomicInteger(0);

    @Getter
    private final AtomicInteger noSyncDbQueryCount = new AtomicInteger(0);

    /**
     * Phương thức lấy thông tin sản phẩm Flash Sale CÓ THUỘC TÍNH sync = true.
     *
     * CƠ CHẾ CHỐNG CACHE STAMPEDE (Thundering Herd Problem):
     * - Khi cache hết hạn hoặc chưa nạp, nếu 50 request cùng ập tới cùng lúc:
     * - sync = true kích hoạt cơ chế khóa đồng bộ (Locking).
     * - CHỈ CÓ DUY NHẤT 1 LUỒNG được thực thi phương thức (truy vấn DB và chờ 2000ms).
     * - 49 luồng còn lại sẽ bị chặn (block) và đợi kết quả từ luồng đầu tiên được ghi vào cache.
     * - Kết quả: Database CHỈ BỊ TRUY VẤN ĐÚNG 1 LẦN!
     */
    @Cacheable(value = "flash-sale", key = "#id", sync = true)
    public FlashSaleProduct getProductByIdWithSync(Long id) {
        dbQueryCount.incrementAndGet();
        log.info("Fetching from Database for product {}", id);

        try {
            // Giả lập thời gian tái tạo cache (truy vấn DB phức tạp) mất 2000ms
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            log.error("Thread sleep interrupted", e);
            Thread.currentThread().interrupt();
        }

        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
    }

    /**
     * Phương thức KHÔNG DÙNG sync (hoặc sync = false) để làm bài toán đối chứng.
     * Khi 50 request đồng thời Cache Miss, cả 50 luồng sẽ cùng lúc nhảy vào CSDL!
     */
    public FlashSaleProduct getProductByIdWithoutSync(Long id) {
        noSyncDbQueryCount.incrementAndGet();
        log.info("[NO SYNC - HIGH LOAD] Fetching from Database for product {} (Total calls: {})", id, noSyncDbQueryCount.get());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại với ID: " + id));
    }

    @CacheEvict(value = "flash-sale", allEntries = true)
    public void clearCache() {
        log.info(">>> Đã xóa sạch cache 'flash-sale' và reset bộ đếm DB Query");
        dbQueryCount.set(0);
        noSyncDbQueryCount.set(0);
    }
}
