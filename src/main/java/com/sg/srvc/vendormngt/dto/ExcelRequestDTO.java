package com.sg.srvc.vendormngt.dto;

import jakarta.validation.constraints.NotBlank;

public class ExcelRequestDTO {

    @NotBlank(message = "File path must not be blank")
    private String filePath;

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
