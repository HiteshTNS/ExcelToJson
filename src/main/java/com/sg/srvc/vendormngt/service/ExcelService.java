package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;

import java.util.Map;

public interface ExcelService {
    ExcelResponseDTO processExcelFile(ExcelRequestDTO requestDTO);  // Change return type to ExcelResponseDTO
}