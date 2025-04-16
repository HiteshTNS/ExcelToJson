package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;

public interface ExcelService {
    ExcelResponseDTO processExcelFile(ExcelRequestDTO requestDTO);
}
