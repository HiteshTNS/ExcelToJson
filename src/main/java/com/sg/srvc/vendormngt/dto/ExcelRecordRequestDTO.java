package com.sg.srvc.vendormngt.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

//RecordDetails File
public class ExcelRecordRequestDTO {
    public Long invoiceFileMasterId;
    public List<InvoiceRecord> invoiceFileRecords;

    public static class InvoiceRecord {
        public String invoiceNumber;
        public String claimNumber;
        public Request request;
        public String status;

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public void setInvoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
        }

        public String getClaimNumber() {
            return claimNumber;
        }

        public void setClaimNumber(String claimNumber) {
            this.claimNumber = claimNumber;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class Request {
        public String companyName;
        public String invoiceNumber;
        public String contractNumber;
        public String claimNumber;
        public String insured;
        public String dateofcompletion;
        public String vin;
        public BigDecimal invoiceamount;

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public void setInvoiceNumber(String invoiceNumber) {
            this.invoiceNumber = invoiceNumber;
        }

        public String getContractNumber() {
            return contractNumber;
        }

        public void setContractNumber(String contractNumber) {
            this.contractNumber = contractNumber;
        }

        public String getClaimNumber() {
            return claimNumber;
        }

        public void setClaimNumber(String claimNumber) {
            this.claimNumber = claimNumber;
        }

        public String getInsured() {
            return insured;
        }

        public void setInsured(String insured) {
            this.insured = insured;
        }

        public String getDateofcompletion() {
            return dateofcompletion;
        }

        public void setDateofcompletion(String dateofcompletion) {
            this.dateofcompletion = dateofcompletion;
        }

        public String getVin() {
            return vin;
        }

        public void setVin(String vin) {
            this.vin = vin;
        }

        public BigDecimal getInvoiceamount() {
            return invoiceamount;
        }

        public void setInvoiceamount(BigDecimal invoiceamount) {
            this.invoiceamount = invoiceamount;
        }
    }

    public Long getInvoiceFileMasterId() {
        return invoiceFileMasterId;
    }

    public void setInvoiceFileMasterId(Long invoiceFileMasterId) {
        this.invoiceFileMasterId = invoiceFileMasterId;
    }

    public List<InvoiceRecord> getInvoiceFileRecords() {
        return invoiceFileRecords;
    }

    public void setInvoiceFileRecords(List<InvoiceRecord> invoiceFileRecords) {
        this.invoiceFileRecords = invoiceFileRecords;
    }
}
