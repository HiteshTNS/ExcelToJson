package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;
import com.sg.srvc.vendormngt.util.ExcelUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ExcelServiceImpl implements ExcelService {

    @Override
    public ExcelResponseDTO processExcelFile(ExcelRequestDTO requestDTO) {
        return ExcelUtil.readAndConvert(requestDTO.getFilePath());
    }
}
