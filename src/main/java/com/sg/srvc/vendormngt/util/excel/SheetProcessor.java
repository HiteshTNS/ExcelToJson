package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.*;

public class SheetProcessor implements XSSFSheetXMLHandler.SheetContentsHandler {

    private final List<InvoiceRecordDTO> records;
    private final List<String> headers = new ArrayList<>();
    private Map<String, Object> currentRowMap;
    private boolean headerRowDetected = false;

    public SheetProcessor(List<InvoiceRecordDTO> records) {
        this.records = records;
    }

    @Override
    public void startRow(int rowNum) {
        currentRowMap = new LinkedHashMap<>(); // temp data comapny details
    }

    @Override
    public void endRow(int rowNum) {
        if (!headerRowDetected) {
            long match = currentRowMap.values().stream()
                    .filter(val -> val instanceof String)
                    .map(val -> ((String) val).toLowerCase())
                    .filter(val -> ExcelHeaderUtils.HEADER_KEYWORDS.stream().anyMatch(val::contains))
                    .count();

            if (match >= 3) {
                currentRowMap.values().forEach(val -> headers.add(ExcelHeaderUtils.mapHeader((String) val)));
                headerRowDetected = true;
            }

        } else if (!headers.isEmpty() && !ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
//            if (ExcelHeaderUtils.isTotalRow(currentRowMap)) return;

            // ✅ Skip rows missing any data in header columns
            boolean hasAllData = headers.stream().allMatch(header -> {
                Object value = currentRowMap.getOrDefault(String.valueOf(headers.indexOf(header)), null);
                return value != null && !value.toString().trim().isEmpty();
            });
            if (!hasAllData) return;

            Map<String, Object> request = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String key = headers.get(i);
                Object value = currentRowMap.getOrDefault(String.valueOf(i), null);
                request.put(key, value);
            }

            InvoiceRecordDTO dto = new InvoiceRecordDTO();
            dto.setClaimNumber(String.valueOf(request.getOrDefault("claim", "")));
            dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoice", "")));
            dto.setRequest(request);
            dto.setStatus("PENDING");
            records.add(dto);
        }
    }

    //This stores cell values into currentRowMap using column index as the key.
    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        int colIndex = ExcelHeaderUtils.getColumnIndex(cellReference); // Converts "A1" → 0, "B1" → 1, etc.
        currentRowMap.put(String.valueOf(colIndex), formattedValue != null ? formattedValue.trim() : "");
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {}
}
