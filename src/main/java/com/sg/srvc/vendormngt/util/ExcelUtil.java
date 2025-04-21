package com.sg.srvc.vendormngt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sg.srvc.vendormngt.dto.ExcelMetadataResponseDTO;
import com.sg.srvc.vendormngt.dto.ExcelRecordRequestDTO;
import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
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

    private static final int BATCH_SIZE = 200;
    private static final List<String> HEADER_KEYWORDS = Arrays.asList("invoice", "contract", "claim", "insured", "date", "vin", "amount");

    public static ExcelResponseDTO readAndConvert(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) throw new CustomException("File not found at path: " + filePath);

            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            // Dynamically find the header row
            int headerRowIndex = findHeaderRow(sheet);
            if (headerRowIndex == -1) throw new CustomException("Header row is missing or invalid");

            // Extract header values
            Row headerRow = sheet.getRow(headerRowIndex);
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValue(cell).trim());
            }

            int invoiceIndex = findHeaderIndexContaining(headers, "Invoice");
            int claimIndex = findHeaderIndexContaining(headers, "Claim");

            List<InvoiceRecordDTO> allRecords = new ArrayList<>();
            int total = 0;

            for (int i = headerRowIndex + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isTotalRow(row) || isEmptyRow(row)) continue;
                total++;

                Map<String, Object> requestMap = new LinkedHashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    requestMap.put(mapHeader(headers.get(j)), getCellValue(cell));
                }

                InvoiceRecordDTO record = new InvoiceRecordDTO();
                record.setRequest(requestMap);

                if (invoiceIndex != -1 && row.getCell(invoiceIndex) != null)
                    record.setInvoiceNumber(getCellValue(row.getCell(invoiceIndex)));
                if (claimIndex != -1 && row.getCell(claimIndex) != null)
                    record.setClaimNumber(getCellValue(row.getCell(claimIndex)));

                record.setStatus("PENDING");
                allRecords.add(record);
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
            meta.setStatus("PENDING");
            meta.setStatusDescription("PENDING");
            meta.setCreatedBy("xyz");
            meta.setUrl("http://dummy-url.com/file/" + meta.getCorrelationId());

            // Batching
            List<ExcelRecordRequestDTO> batches = new ArrayList<>();
            for (int i = 0; i < allRecords.size(); i += BATCH_SIZE) {
                List<InvoiceRecordDTO> batch = allRecords.subList(i, Math.min(i + BATCH_SIZE, allRecords.size()));
                ExcelRecordRequestDTO dto = new ExcelRecordRequestDTO();
                dto.setInvoiceFileMasterId(new Random().nextLong(100000, 999999));
                dto.setInvoiceFileRecords(batch);
                batches.add(dto);
            }

            ExcelResponseDTO response = new ExcelResponseDTO();
            response.setMetadata(meta);
            response.setRecordDetails(batches);

            String outputPath = "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel";
            String savedFile = saveJsonToFile(response, outputPath, "invoice_data");
            System.out.println("✅ JSON saved at: " + savedFile);

            return response;

        } catch (Exception e) {
            throw new CustomException("Error parsing Excel file: " + e.getMessage());
        }
    }

    // Function to dynamically find the header row
    private static int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null) {
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    String cellValue = getCellValue(row.getCell(j)).toLowerCase();
                    for (String keyword : HEADER_KEYWORDS) {
                        if (cellValue.contains(keyword)) {
                            return i; // Found header row
                        }
                    }
                }
            }
        }
        return -1; // No valid header row found
    }

    private static boolean isEmptyRow(Row row) {
        for (Cell cell : row) {
            if (cell != null && !getCellValue(cell).trim().isEmpty()) return false;
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

    private static String mapHeader(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) return "";

        // Remove all non-alphanumeric characters (excluding spaces and underscores)
        String cleaned = rawHeader.replaceAll("[^a-zA-Z0-9\\s_]", "");

        // Convert to lowercase and split by space or underscore
        String[] parts = cleaned.trim().toLowerCase().split("[\\s_]+");
        if (parts.length == 0) return "";

        StringBuilder result = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            result.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }

        return result.toString();
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
