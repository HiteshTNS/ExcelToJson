package com.sg.srvc.vendormngt.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExcelRequestDTO {

    @NotBlank(message = "File Name must not be blank")
    private String fileName;
    @NotBlank(message = "correlationId must not be blank")
    private String correlationId;
    @NotBlank(message = "vendorCode must not be blank")
    private String vendorCode;

}
