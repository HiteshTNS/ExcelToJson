package com.sg.srvc.vendormngt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sg.srvc.vendormngt.dto.ExcelMetadataResponseDTO;
import com.sg.srvc.vendormngt.dto.ExcelRecordRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;
import com.sg.srvc.vendormngt.exception.CustomException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExcelUtil {

    public static ExcelResponseDTO readAndConvert(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) throw new CustomException("File not found at path: " + filePath);

            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            List<ExcelRecordRequestDTO.InvoiceRecord> records = new ArrayList<>();
            int total = 0;
            for (int i = 8; i <= sheet.getLastRowNum(); i++) { // Row 9 = index 8
                Row row = sheet.getRow(i);
                if (row == null || isTotalRow(row) || isEmptyRow(row)) continue;
                total++;
                ExcelRecordRequestDTO.InvoiceRecord record = new ExcelRecordRequestDTO.InvoiceRecord();
                ExcelRecordRequestDTO.Request request = new ExcelRecordRequestDTO.Request();

                request.companyName = getCellValue(row.getCell(0));
                request.invoiceNumber = getCellValue(row.getCell(1));
                request.contractNumber = getCellValue(row.getCell(2));
                request.claimNumber = getCellValue(row.getCell(3));
                request.insured = getCellValue(row.getCell(4));
                request.dateofcompletion = parseDate(row.getCell(5));
                request.vin = getCellValue(row.getCell(6));
                request.invoiceamount = parseBigDecimal(row.getCell(7));

                record.request = request;
                record.invoiceNumber = request.invoiceNumber;
                record.claimNumber = request.claimNumber;
                record.status = "PENDING";
                records.add(record);
            }
            workbook.close();

            ExcelMetadataResponseDTO meta = new ExcelMetadataResponseDTO();
            meta.correlationId = UUID.randomUUID().toString();
            meta.vendorCode = "xyz";
            meta.name = file.getName();
            meta.fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
            meta.totalRecCount = total;
            meta.successRecCount = total;
            meta.pendingRecCount = 0;
            meta.errorRecCount = 0;
            meta.status = "pending";
            meta.statusDescription = "pending";
            meta.createdBy = "xyz";
            meta.url = "http://dummy-url.com/file/" + meta.correlationId;

            ExcelRecordRequestDTO recordDTO = new ExcelRecordRequestDTO();
            recordDTO.invoiceFileMasterId = new Random().nextLong(100000, 999999);
            recordDTO.invoiceFileRecords = records;

            ExcelResponseDTO response = new ExcelResponseDTO();
            response.setMetadata(meta);
            response.setRecordDetails(recordDTO);

            // Save JSON to disk
            String outputPath = "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel"; // Change if needed
            String savedFile = saveJsonToFile(response, outputPath, "invoice_data");
            System.out.println("✅ JSON saved at: " + savedFile);

            return response;

        } catch (Exception e) {
            throw new CustomException("Error parsing Excel file: " + e.getMessage());
        }
    }



    private static boolean isEmptyRow(Row row) {
        for (int i = 0; i < 8; i++) {
            if (row.getCell(i) != null && !getCellValue(row.getCell(i)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTotalRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (getCellValue(row.getCell(i)).toLowerCase().contains("total")) return true;
        }
        return false;
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    // Preserve decimals exactly for amount and large numbers as plain string
                    return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }



    private static String parseDate(Cell cell) {
        try {
            if (cell == null) return null;
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return new SimpleDateFormat("MM/dd/yyyy").format(cell.getDateCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                String raw = cell.getStringCellValue().trim();
                Date parsed = new SimpleDateFormat("MM/dd/yyyy").parse(raw);
                return new SimpleDateFormat("MM/dd/yyyy").format(parsed);
            }
        } catch (Exception e) {
            e.printStackTrace(); // logging if needed
        }
        return null;
    }



    private static BigDecimal parseBigDecimal(Cell cell) {
        try {
            return new BigDecimal(getCellValue(cell));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }


    public static String saveJsonToFile(Object jsonObj, String outputDir, String filePrefix) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = filePrefix + "_" + timestamp + ".json";
        File file = new File(outputDir, fileName);

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonObj);
        return file.getAbsolutePath();
    }

}
