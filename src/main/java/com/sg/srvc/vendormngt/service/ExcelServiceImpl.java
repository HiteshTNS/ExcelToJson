package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.InvoiceFileResponseDTO;
import com.sg.srvc.vendormngt.util.excel.FileReaderUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;

@ApplicationScoped
public class ExcelServiceImpl implements ExcelService {

    private final FileReaderUtil fileReaderUtil;

    @ConfigProperty(name = "excel.base.path")
    String baseDir;

    public ExcelServiceImpl(FileReaderUtil fileReaderUtil) {
        this.fileReaderUtil = fileReaderUtil;
    }

    @Override
    public InvoiceFileResponseDTO processExcelFile(ExcelRequestDTO requestDTO) throws Exception {
        String fileName = requestDTO.getFileName();
        String fullPath = baseDir + File.separator + fileName;
        return fileReaderUtil.readAndConvert(fullPath, requestDTO.getCorrelationId(), requestDTO.getVendorCode());

    }
}
