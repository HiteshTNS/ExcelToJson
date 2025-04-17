package com.sg.srvc.vendormngt.dto;

//Meta data class
public class ExcelMetadataResponseDTO {
    public String correlationId;
    public String vendorCode;
    public String name;
    public String fileExtension;
    public long totalRecCount;
    public long successRecCount;
    public long pendingRecCount;
    public long errorRecCount;
    public String url;
    public String status;
    public String statusDescription;
    public String createdBy;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    public void setVendorCode(String vendorCode) {
        this.vendorCode = vendorCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotalRecCount() {
        return totalRecCount;
    }

    public void setTotalRecCount(long totalRecCount) {
        this.totalRecCount = totalRecCount;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public long getPendingRecCount() {
        return pendingRecCount;
    }

    public void setPendingRecCount(long pendingRecCount) {
        this.pendingRecCount = pendingRecCount;
    }

    public long getSuccessRecCount() {
        return successRecCount;
    }

    public void setSuccessRecCount(long successRecCount) {
        this.successRecCount = successRecCount;
    }

    public long getErrorRecCount() {
        return errorRecCount;
    }

    public void setErrorRecCount(long errorRecCount) {
        this.errorRecCount = errorRecCount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
