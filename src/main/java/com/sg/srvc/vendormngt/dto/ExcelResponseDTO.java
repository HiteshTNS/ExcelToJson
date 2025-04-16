package com.sg.srvc.vendormngt.dto;

import java.util.List;

public class ExcelResponseDTO {

    private FileDetails fileDetails;
    private List<RecordDetailsDTO> recordDetails;

    public static class FileDetails {
        private String fileName;
        private String fileExtension;
        private int totalRecords;

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }

        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    }

    public FileDetails getFileDetails() { return fileDetails; }
    public void setFileDetails(FileDetails fileDetails) { this.fileDetails = fileDetails; }

    public List<RecordDetailsDTO> getRecordDetails() { return recordDetails; }
    public void setRecordDetails(List<RecordDetailsDTO> recordDetails) { this.recordDetails = recordDetails; }
}
