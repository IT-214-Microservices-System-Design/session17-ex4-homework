package com.grabfood.flashsale.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Entity
@Table(name = "flash_sale_products")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleProduct implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @Column(name = "name", nullable = false)
    @JsonProperty("name")
    private String name;

    @Column(name = "original_price", nullable = false)
    @JsonProperty("original_price")
    private Double originalPrice;

    @Column(name = "sale_price", nullable = false)
    @JsonProperty("sale_price")
    private Double salePrice;

    @Column(name = "stock", nullable = false)
    @JsonProperty("stock")
    private Integer stock;
}
