package com.sg.srvc.vendormngt.dto;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class InvoiceRecordDTO {
    private String invoiceNumber;
    private String claimNumber;
    private Map<String, Object> request; // Now dynamic
    private String status;

    // Getters and Setters
//    public String getInvoiceNumber() {
//        return invoiceNumber;
//    }
//
//    public void setInvoiceNumber(String invoiceNumber) {
//        this.invoiceNumber = invoiceNumber;
//    }
//
//    public String getClaimNumber() {
//        return claimNumber;
//    }
//
//    public void setClaimNumber(String claimNumber) {
//        this.claimNumber = claimNumber;
//    }
//
//    public Map<String, Object> getRequest() {
//        return request;
//    }
//
//    public void setRequest(Map<String, Object> request) {
//        this.request = request;
//    }
//
//    public String getStatus() {
//        return status;
//    }
//
//    public void setStatus(String status) {
//        this.status = status;
//    }
}
