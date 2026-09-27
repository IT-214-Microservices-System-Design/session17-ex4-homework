package com.grabfood.flashsale.controller;

import com.grabfood.flashsale.entity.FlashSaleProduct;
import com.grabfood.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    /**
     * API tra cứu 1 sản phẩm Flash Sale: GET /flash-sale/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlashSaleProduct> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(flashSaleService.getProductByIdWithSync(id));
    }

    /**
     * API Giả lập Kịch bản Cache Stampede với 50 request đồng thời (CÓ sync = true):
     * POST /flash-sale/simulate-stampede/{id}?concurrentRequests=50
     */
    @PostMapping("/simulate-stampede/{id}")
    public ResponseEntity<Map<String, Object>> simulateStampede(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int concurrentRequests) {

        // Xóa cache trước để mô phỏng sự kiện Cache Miss đồng loạt
        flashSaleService.clearCache();

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentRequests; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    FlashSaleProduct product = flashSaleService.getProductByIdWithSync(id);
                    if (product != null) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    log.error("Request lỗi trong quá trình test đa luồng: ", e);
                    failureCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        // Chờ tất cả 50 luồng hoàn tất
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long totalExecutionTime = System.currentTimeMillis() - startTime;

        executor.shutdown();

        Map<String, Object> response = new HashMap<>();
        response.put("mode", "VỚI THUỘC TÍNH sync = true (ĐÃ BẢO VỆ CHỐNG CACHE STAMPEDE)");
        response.put("productId", id);
        response.put("concurrentRequestsSent", concurrentRequests);
        response.put("successfulResponses", successCount.get());
        response.put("failedResponses", failureCount.get());
        response.put("databaseQueryCalls", flashSaleService.getDbQueryCount().get());
        response.put("totalExecutionTimeMs", totalExecutionTime);
        response.put("resultExplanation", "Mặc dù có " + concurrentRequests + " luồng gọi đồng thời, Database CHỈ BỊ TRUY VẤN ĐÚNG "
                + flashSaleService.getDbQueryCount().get() + " LẦN nhờ cơ chế khóa của sync = true! Tổng thời gian hoàn thành ~ "
                + totalExecutionTime + "ms (xấp xỉ thời gian của 1 query đơn lẻ ~2000ms).");

        return ResponseEntity.ok(response);
    }

    /**
     * API Giả lập Kịch bản KHÔNG CÓ sync = true (Bị Cache Stampede):
     * POST /flash-sale/simulate-without-sync/{id}?concurrentRequests=50
     */
    @PostMapping("/simulate-without-sync/{id}")
    public ResponseEntity<Map<String, Object>> simulateWithoutSync(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int concurrentRequests) {

        flashSaleService.clearCache();

        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < concurrentRequests; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                FlashSaleProduct product = flashSaleService.getProductByIdWithoutSync(id);
                if (product != null) {
                    successCount.incrementAndGet();
                }
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        long totalExecutionTime = System.currentTimeMillis() - startTime;

        executor.shutdown();

        Map<String, Object> response = new HashMap<>();
        response.put("mode", "KHÔNG DÙNG sync = true (BỊ NGHẼN VÀ QUÁ TẢI DATABASE)");
        response.put("productId", id);
        response.put("concurrentRequestsSent", concurrentRequests);
        response.put("successfulResponses", successCount.get());
        response.put("databaseQueryCalls", flashSaleService.getNoSyncDbQueryCount().get());
        response.put("totalExecutionTimeMs", totalExecutionTime);
        response.put("resultExplanation", "Khi KHÔNG DÙNG sync = true, toàn bộ " + flashSaleService.getNoSyncDbQueryCount().get()
                + " request đều đổ dồn truy vấn Database cùng lúc, gây hiện tượng nghẽn Database (Cache Stampede).");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        flashSaleService.clearCache();
        return ResponseEntity.ok(Map.of("message", "Đã xóa toàn bộ cache và reset counters"));
    }
}
