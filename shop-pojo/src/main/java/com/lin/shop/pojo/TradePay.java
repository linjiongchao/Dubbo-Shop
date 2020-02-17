package com.lin.shop.pojo;

import java.io.Serializable;
import java.math.BigDecimal;

public class TradePay extends TradePayKey implements Serializable {
    private BigDecimal payAmount;

    private Integer isPaid;

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public Integer getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Integer isPaid) {
        this.isPaid = isPaid;
    }
}