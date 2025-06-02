package com.sg.srvc.vendormngt.service;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.InvoiceFileResponseDTO;
import com.sg.srvc.vendormngt.util.excel.FileReaderUtil;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;

@ApplicationScoped
public class ExcelServiceImpl implements ExcelService {

    @ConfigProperty(name = "excel.base.path")
    String baseDir;

    @Override
    public InvoiceFileResponseDTO processExcelFile(ExcelRequestDTO requestDTO) {
//        System.out.println("File Name : " + requestDTO.getFileName() );
//        System.out.println("Corelation ID  : " + requestDTO.getCorrelationId() );
//        System.out.println("Vendor Code : " + requestDTO.getVendorCode() );
        String fileName = requestDTO.getFileName();
        String fullPath = baseDir + File.separator + fileName;

//        return FileReaderUtil.readAndConvert(fullPath,requestDTO.getCorrelationId(), requestDTO.getVendorCode());
        try {
            return FileReaderUtil.readAndConvert(fullPath, requestDTO.getCorrelationId(), requestDTO.getVendorCode());
        } catch (Exception e) {
            throw new RuntimeException("Error while reading and converting file: " + e.getMessage(), e);
        }

    }
}
