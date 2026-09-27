package com.grabfood.flashsale;

import com.grabfood.flashsale.entity.FlashSaleProduct;
import com.grabfood.flashsale.service.FlashSaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CacheStampedeSyncTest {

    @Autowired
    private FlashSaleService flashSaleService;

    @BeforeEach
    void setUp() {
        flashSaleService.clearCache();
    }

    @Test
    @DisplayName("Kiểm thử 50 luồng đồng thời: Với sync = true, chỉ DUY NHẤT 1 lần truy vấn CSDL xảy ra")
    void testConcurrent50RequestsWithSyncAttribute() {
        int numberOfThreads = 50;
        Long productId = 1L;

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<FlashSaleProduct>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Kích hoạt 50 luồng đồng thời gọi tới cùng 1 ID sản phẩm
        for (int i = 0; i < numberOfThreads; i++) {
            CompletableFuture<FlashSaleProduct> future = CompletableFuture.supplyAsync(
                    () -> flashSaleService.getProductByIdWithSync(productId),
                    executorService
            );
            futures.add(future);
        }

        // Chờ tất cả 50 luồng hoàn thành
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long totalDuration = System.currentTimeMillis() - startTime;

        executorService.shutdown();

        // 1. Kiểm tra tất cả 50 request đều nhận được kết quả chính xác
        for (CompletableFuture<FlashSaleProduct> future : futures) {
            FlashSaleProduct product = future.join();
            assertNotNull(product, "Kết quả trả về không được null");
            assertEquals(productId, product.getId());
            assertEquals("Combo Ga Ran Sieu Cay Giam 50%", product.getName());
            assertEquals(75000.0, product.getSalePrice());
        }

        // 2. Tiêu chí quan trọng nhất: DUY NHẤT 1 LẦN TRUY VẤN CSDL XẢY RA
        assertEquals(1, flashSaleService.getDbQueryCount().get(),
                "Với sync = true, CSDL chỉ được phép truy vấn duy nhất 1 lần cho 50 request đồng thời!");

        // 3. Kiểm tra tổng thời gian: Xấp xỉ thời gian của 1 query (~2000ms), không phải 50 x 2000ms = 100.000ms
        assertTrue(totalDuration < 6000,
                "Tổng thời gian phải xấp xỉ ~2s (cộng chút overhead luồng), thực tế: " + totalDuration + "ms");

        System.out.println(">>> KIỂM THỬ THÀNH CÔNG: 50 requests hoàn tất trong " + totalDuration
                + "ms với duy nhất " + flashSaleService.getDbQueryCount().get() + " lần truy vấn DB.");
    }
}
