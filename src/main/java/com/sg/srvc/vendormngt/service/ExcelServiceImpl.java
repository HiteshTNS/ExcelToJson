package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;
import com.sg.srvc.vendormngt.util.ExcelUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ExcelServiceImpl implements ExcelService {

    @Inject
    ExcelUtil excelUtil;

    @Override
    public ExcelResponseDTO processExcelFile(ExcelRequestDTO requestDTO) {
        return excelUtil.readAndConvert(requestDTO.getFilePath());
    }
}
