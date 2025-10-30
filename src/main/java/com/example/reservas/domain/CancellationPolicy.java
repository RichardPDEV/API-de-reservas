package com.example.reservas.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "cancellation_policy")
public class CancellationPolicy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="business_id", nullable=false)
    private Business business;

    @Column(name="free_before_minutes", nullable=false)
    private Integer freeBeforeMinutes;

    @Column(name="penalty_type", nullable=false)
    private String penaltyType;

    @Column(name="penalty_amount", nullable=false)
    private Double penaltyAmount;

    // getters/setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public Integer getFreeBeforeMinutes() {
        return freeBeforeMinutes;
    }

    public void setFreeBeforeMinutes(Integer freeBeforeMinutes) {
        this.freeBeforeMinutes = freeBeforeMinutes;
    }

    public String getPenaltyType() {
        return penaltyType;
    }

    public void setPenaltyType(String penaltyType) {
        this.penaltyType = penaltyType;
    }

    public Double getPenaltyAmount() {
        return penaltyAmount;
    }

    public void setPenaltyAmount(Double penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }

    

    
}