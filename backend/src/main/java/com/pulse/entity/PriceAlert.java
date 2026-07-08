package com.pulse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_alerts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String symbol;

    private String companyName;

    @Column(name = "target_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal targetPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertCondition condition;

    @Builder.Default
    @Column(nullable = false)
    private boolean triggered = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
