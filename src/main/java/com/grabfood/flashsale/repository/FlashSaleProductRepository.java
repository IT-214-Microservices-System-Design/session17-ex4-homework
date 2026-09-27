package com.grabfood.flashsale.repository;

import com.grabfood.flashsale.entity.FlashSaleProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlashSaleProductRepository extends JpaRepository<FlashSaleProduct, Long> {
}
