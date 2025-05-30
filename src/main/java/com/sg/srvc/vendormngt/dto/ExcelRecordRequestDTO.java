package com.sg.srvc.vendormngt.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ExcelRecordRequestDTO {
    private List<InvoiceRecordDTO> invoiceFileRecords;

    // Getters and Setters
//    public Long getInvoiceFileMasterId() {
//        return invoiceFileMasterId;
//    }
//
//    public void setInvoiceFileMasterId(Long invoiceFileMasterId) {
//        this.invoiceFileMasterId = invoiceFileMasterId;
//    }
//
//    public List<InvoiceRecordDTO> getInvoiceFileRecords() {
//        return invoiceFileRecords;
//    }
//
//    public void setInvoiceFileRecords(List<InvoiceRecordDTO> invoiceFileRecords) {
//        this.invoiceFileRecords = invoiceFileRecords;
//    }
}
