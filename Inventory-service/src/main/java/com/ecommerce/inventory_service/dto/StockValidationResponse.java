package com.ecommerce.inventory_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockValidationResponse {
    private Long productId;
    private Boolean isAvailable;
    private Integer availableQuantity;
    private Integer requestedQuantity;
    private String message;

//    // All-args constructor
//    public StockValidationResponse(Long productId, Boolean isAvailable, Integer availableQuantity,
//                                   Integer requestedQuantity, String message) {
//        this.productId = productId;
//        this.isAvailable = isAvailable;
//        this.availableQuantity = availableQuantity;
//        this.requestedQuantity = requestedQuantity;
//        this.message = message;
//    }
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Integer requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
