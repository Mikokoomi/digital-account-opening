package com.digitalbank.accountopening.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "min_age", nullable = false)
    private Integer minAge;

    @Column(name = "allow_multiple_accounts", nullable = false)
    private Boolean allowMultipleAccounts;

    @Column(name = "requires_manual_review", nullable = false)
    private Boolean requiresManualReview;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
