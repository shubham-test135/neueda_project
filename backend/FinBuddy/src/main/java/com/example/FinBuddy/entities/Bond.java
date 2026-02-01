package com.example.FinBuddy.entities;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bond entity representing fixed-income investments
 */
@Entity
@DiscriminatorValue("BOND")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Bond extends Asset {

    @Column(name = "coupon_rate", precision = 10, scale = 4)
    private BigDecimal couponRate; // Annual interest rate

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "face_value", precision = 19, scale = 2)
    private BigDecimal faceValue;

    @Column(name = "bond_type")
    private String bondType; // e.g., Government, Corporate, Municipal

    @Column(name = "issuer")
    private String issuer; // Bond issuer name

    @Column(name = "credit_rating")
    private String creditRating; // e.g., AAA, AA, A

    @Override
    public String getAssetType() {
        return "BOND";
    }
}
