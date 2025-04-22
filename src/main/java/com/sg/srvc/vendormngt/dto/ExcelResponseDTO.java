package com.sg.srvc.vendormngt.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ExcelResponseDTO {
    public ExcelMetadataResponseDTO metadata;
    private List<ExcelRecordRequestDTO> recordDetails; // Batched

    // Getters and Setters
//    public ExcelMetadataResponseDTO getMetadata() {
//        return metadata;
//    }
//
//    public void setMetadata(ExcelMetadataResponseDTO metadata) {
//        this.metadata = metadata;
//    }
//
//    public ExcelRecordRequestDTO getRecordDetails() {
//        return recordDetails;
//    }
//
//    public void setRecordDetails(ExcelRecordRequestDTO recordDetails) {
//        this.recordDetails = recordDetails;
//    }
}
