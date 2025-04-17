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

            // Get header row (index 7 = Excel row 8)
            Row headerRow = sheet.getRow(7);
            if (headerRow == null) throw new CustomException("Header row is missing at row 8");

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell).trim());
            }

            int invoiceIndex = findHeaderIndexContaining(headers, "Invoice");
            int claimIndex = findHeaderIndexContaining(headers, "Claim");

            List<ExcelRecordRequestDTO.InvoiceRecord> records = new ArrayList<>();
            int total = 0;

            for (int i = 8; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isTotalRow(row) || isEmptyRow(row)) continue;
                total++;

                Map<String, Object> requestMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    requestMap.put(mapHeader(headers.get(j)), getCellValue(cell));
                }

                ExcelRecordRequestDTO.InvoiceRecord record = new ExcelRecordRequestDTO.InvoiceRecord();
                record.setRequest(requestMap);

                // Extract invoice and claim number
                if (invoiceIndex != -1 && row.getCell(invoiceIndex) != null)
                    record.setInvoiceNumber(getCellValue(row.getCell(invoiceIndex)));
                if (claimIndex != -1 && row.getCell(claimIndex) != null)
                    record.setClaimNumber(getCellValue(row.getCell(claimIndex)));

                record.setStatus("PENDING");
                records.add(record);
            }

            workbook.close();

            // Metadata
            ExcelMetadataResponseDTO meta = new ExcelMetadataResponseDTO();
            meta.setCorrelationId(UUID.randomUUID().toString());
            meta.setVendorCode("xyz");
            meta.setName(file.getName());
            meta.setFileExtension(file.getName().substring(file.getName().lastIndexOf('.') + 1));
            meta.setTotalRecCount(total);
            meta.setSuccessRecCount(total);
            meta.setPendingRecCount(0);
            meta.setErrorRecCount(0);
            meta.setStatus("pending");
            meta.setStatusDescription("pending");
            meta.setCreatedBy("xyz");
            meta.setUrl("http://dummy-url.com/file/" + meta.getCorrelationId());

            ExcelRecordRequestDTO recordDTO = new ExcelRecordRequestDTO();
            recordDTO.setInvoiceFileMasterId(new Random().nextLong(100000, 999999));
            recordDTO.setInvoiceFileRecords(records);

            ExcelResponseDTO response = new ExcelResponseDTO();
            response.setMetadata(meta);
            response.setRecordDetails(recordDTO);

            // Save JSON to disk
            String outputPath = "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel";
            String savedFile = saveJsonToFile(response, outputPath, "invoice_data");
            System.out.println("✅ JSON saved at: " + savedFile);

            return response;

        } catch (Exception e) {
            throw new CustomException("Error parsing Excel file: " + e.getMessage());
        }
    }

    private static boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && !getCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isTotalRow(Row row) {
        for (Cell cell : row) {
            if (getCellValue(cell).toLowerCase().contains("total")) return true;
        }
        return false;
    }

    private static String getCellValue(Cell cell) {
        try {
            if (cell == null) return "";
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return new SimpleDateFormat("MM/dd/yyyy").format(cell.getDateCellValue());
                    } else {
                        return BigDecimal.valueOf(cell.getNumericCellValue()).toPlainString();
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    return getCellValue(evaluateFormula(cell));
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static Cell evaluateFormula(Cell cell) {
        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
        return evaluator.evaluateInCell(cell);
    }

    private static int findHeaderIndexContaining(List<String> headers, String keyword) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).toLowerCase().contains(keyword.toLowerCase())) return i;
        }
        return -1;
    }

    private static String mapHeader(String raw) {
        raw = raw.toLowerCase();
        if (raw.contains("company")) return "companyName";
        if (raw.contains("contract")) return "contractNumber";
        if (raw.contains("claim")) return "claimNumber";
        if (raw.contains("insured")) return "insured";
        if (raw.contains("date")) return "dateOfCompletion";
        if (raw.contains("vin")) return "vin";

        if (raw.contains("invoice") && raw.contains("amount")) return "invoiceAmount";
        if (raw.contains("invoice")) return "invoiceNumber";

        return raw.replaceAll("\\s+", "_");
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
