package com.sg.srvc.vendormngt.dto;

import lombok.Data;

@Data
public class RecJsonDTO {
    private String companyName;
    private String invoiceNumber;
    private String contractNumber;
    private String claimNumber;
    private String insured;
    private String dateOfCompletion;
    private String vin;
    private String invoiceAmount;
    private String policyNumber;
    private String miles;
    private String inspectionFee;
    private String fee;
    private String charges;
    private String checkNumber;

    // getters and setters
}