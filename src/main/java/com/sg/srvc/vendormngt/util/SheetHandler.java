package com.sg.srvc.vendormngt.util;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.*;

public class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {

    private final List<InvoiceRecordDTO> recordList;
    private final List<String> headers = new ArrayList<>();
    private Map<String, Object> currentRowMap;
    private boolean headerRowDetected = false;
    private int currentRow = -1;

    public SheetHandler(List<InvoiceRecordDTO> records) {
        this.recordList = records;
    }

    @Override
    public void startRow(int rowNum) {
        currentRow = rowNum;
        currentRowMap = new LinkedHashMap<>();
    }

    @Override
    public void endRow(int rowNum) {
        if (!headerRowDetected) {
            long matchCount = currentRowMap.values().stream()
                    .filter(val -> val instanceof String)
                    .map(val -> ((String) val).toLowerCase())
                    .filter(val -> ExcelUtil.HEADER_KEYWORDS.stream().anyMatch(val::contains))
                    .count();

            if (matchCount >= 3) {
                currentRowMap.values().forEach(val -> headers.add(ExcelUtil.mapHeader((String) val)));
                headerRowDetected = true;
            }
        } else if (!headers.isEmpty() && !isEmptyRow(currentRowMap)) {

            if (isTotalRow(currentRowMap)) {
                System.out.println("Skipping Total row: " + currentRowMap);
                return;  // Skip this row
            }

            Map<String, Object> request = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String key = headers.get(i);
                if (key != null && !key.isBlank()) {
                    Object value = currentRowMap.getOrDefault(String.valueOf(i), null);
                    request.put(key, value);
                }
            }

            // Debugging the current row data
//            System.out.println("Current Row Data: " + currentRowMap);
//            System.out.println("Mapped Request Data: " + request);
            InvoiceRecordDTO dto = new InvoiceRecordDTO();
            dto.setClaimNumber(String.valueOf(request.getOrDefault("claim", "")));
            dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoice", "")));
            dto.setRequest(request);
            dto.setStatus("PENDING");
            recordList.add(dto);
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        if (cellReference == null) return;
        int colIndex = getColumnIndex(cellReference);
        currentRowMap.put(String.valueOf(colIndex), formattedValue != null ? formattedValue.trim() : "");
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {
        // Not used
    }

    private boolean isEmptyRow(Map<String, Object> row) {
        return row.values().stream().allMatch(val -> val == null || val.toString().trim().isEmpty());
    }

    private int getColumnIndex(String cellRef) {
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (Character.isDigit(ch)) break;
            col = col * 26 + (ch - 'A' + 1);
        }
        return col - 1;
    }

    // Helper method to check if the row is a total row
    private boolean isTotalRow(Map<String, Object> row) {
        // Check if the row contains the word "Total" or any other specific keyword or value
        for (Object value : row.values()) {
            if (value != null && value.toString().toLowerCase().contains("total")) {
                return true;  // This is a total row, skip it
            }
        }
        return false;  // This is not a total row
    }
}
