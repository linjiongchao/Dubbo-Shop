package com.lin.shop.pojo;

import java.io.Serializable;

public class TradePayKey implements Serializable {
    private Long payId;

    private Long orderId;

    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}