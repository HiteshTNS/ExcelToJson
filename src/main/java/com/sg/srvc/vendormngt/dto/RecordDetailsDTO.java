package com.sg.srvc.vendormngt.dto;

import java.math.BigDecimal;

public class RecordDetailsDTO {
    private String companyName;
    private String invoice;
    private String contract;
    private String claim;
    private String insured;
    private String dateOfCompletion;
    private String vin;
    private BigDecimal invoiceAmount;

    // Getters and Setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getInvoice() { return invoice; }
    public void setInvoice(String invoice) { this.invoice = invoice; }

    public String getContract() { return contract; }
    public void setContract(String contract) { this.contract = contract; }

    public String getClaim() { return claim; }
    public void setClaim(String claim) { this.claim = claim; }

    public String getInsured() { return insured; }
    public void setInsured(String insured) { this.insured = insured; }

    public String getDateOfCompletion() { return dateOfCompletion; }
    public void setDateOfCompletion(String dateOfCompletion) { this.dateOfCompletion = dateOfCompletion; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public BigDecimal getInvoiceAmount() { return invoiceAmount; }
    public void setInvoiceAmount(BigDecimal invoiceAmount) { this.invoiceAmount = invoiceAmount; }
}
