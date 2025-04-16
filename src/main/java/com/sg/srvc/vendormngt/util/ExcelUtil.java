package com.sg.srvc.vendormngt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;
import com.sg.srvc.vendormngt.dto.RecordDetailsDTO;
import com.sg.srvc.vendormngt.exception.CustomException;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;

@ApplicationScoped
public class ExcelUtil {

    public ExcelResponseDTO readAndConvert(String filePath) {
        List<RecordDetailsDTO> records = new ArrayList<>();
        ExcelResponseDTO responseDTO = new ExcelResponseDTO();
        ExcelResponseDTO.FileDetails fileDetails = new ExcelResponseDTO.FileDetails();

        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            throw new CustomException("File not found at path: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            // Use getLastRowNum to get the last row index (including blank rows)
            int totalRows = sheet.getLastRowNum() + 1; // +1 to account for 0-based index
            System.out.println("Total rows in sheet: " + totalRows); // Debug the row count


            // Loop through the rows, start from index 8 (row 9), and exclude the last row
            for (int i = 8; i < totalRows; i++) { //Skipping the last row total
                Row row = sheet.getRow(i);

                if (row != null) {
                    String totalFlag = getCellValue(row.getCell(6)); // Assuming column G contains the "total" value
                    if (totalFlag != null && totalFlag.toLowerCase().contains("total")) {
                        break; // Stop processing if this is the total row
                    }
                    RecordDetailsDTO record = new RecordDetailsDTO();
                    record.setCompanyName(getCellValue(row.getCell(0)));
                    record.setInvoice(getCellValue(row.getCell(1)));
                    record.setContract(getCellValue(row.getCell(2)));
                    record.setClaim(getCellValue(row.getCell(3)));
                    record.setInsured(getCellValue(row.getCell(4)));
                    record.setDateOfCompletion(getCellValue(row.getCell(5)));
                    record.setVin(getCellValue(row.getCell(6)));

                    Cell cell = row.getCell(7);
                    BigDecimal amount;

                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        amount = BigDecimal.valueOf(cell.getNumericCellValue());
                    } else {
                        String amountStr = getCellValue(cell); // fallback for string type
                        amount = new BigDecimal(amountStr);
                    }
                    record.setInvoiceAmount(amount);


                    records.add(record);
                }
            }

            fileDetails.setFileName(inputFile.getName());
            fileDetails.setFileExtension(getExtension(inputFile));
            fileDetails.setTotalRecords(records.size());

            responseDTO.setFileDetails(fileDetails);
            responseDTO.setRecordDetails(records);

            saveAsJsonFile(responseDTO);
            return responseDTO;

        } catch (Exception e) {
            throw new CustomException("Failed to process Excel file: " + e.getMessage());
        }
    }

    private String getExtension(File file) {
        String name = file.getName();
        return name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? new SimpleDateFormat("MM/dd/yyyy").format(cell.getDateCellValue())
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private void saveAsJsonFile(ExcelResponseDTO response) {
        String directoryPath = "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel";
        Path outputPath = Paths.get(directoryPath, "output_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".json");

        try {
            Files.createDirectories(outputPath.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                writer.write(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(response));
            }
        } catch (IOException e) {
            // Handle the exception gracefully
            throw new CustomException("Failed to save JSON file: " + e.getMessage());
        }
    }


}
