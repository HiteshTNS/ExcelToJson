package com.sg.srvc.vendormngt.dto;

import java.util.List;

public class ExcelResponseDTO {
    public ExcelMetadataResponseDTO metadata;
    public ExcelRecordRequestDTO recordDetails;

    // Getters and Setters
    public ExcelMetadataResponseDTO getMetadata() {
        return metadata;
    }

    public void setMetadata(ExcelMetadataResponseDTO metadata) {
        this.metadata = metadata;
    }

    public ExcelRecordRequestDTO getRecordDetails() {
        return recordDetails;
    }

    public void setRecordDetails(ExcelRecordRequestDTO recordDetails) {
        this.recordDetails = recordDetails;
    }
}
