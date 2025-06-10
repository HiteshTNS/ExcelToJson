package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.InvoiceFileResponseDTO;

public interface ExcelService {
    InvoiceFileResponseDTO processExcelFile(ExcelRequestDTO requestDTO) throws Exception;  // Change return type to ExcelResponseDTO
}