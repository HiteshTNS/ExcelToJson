package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.*;

public class SheetProcessor implements XSSFSheetXMLHandler.SheetContentsHandler {

    private final List<InvoiceRecordDTO> records;
    private final String vendorCode;
    private final Map<String, String> excelToInternalMap;
    private final List<String> expectedHeaders;  // expected header sequence
    private Map<Integer, String> columnIndexToHeaderMap = new HashMap<>();
    private Map<String, Object> currentRowMap;
    private boolean headerRowDetected = false;

    public SheetProcessor(List<InvoiceRecordDTO> records, String vendorCode) {
        this.records = records;
        this.vendorCode = vendorCode;
        this.excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
        this.expectedHeaders = new ArrayList<>(excelToInternalMap.keySet());
    }

    @Override
    public void startRow(int rowNum) {
        currentRowMap = new HashMap<>();
    }

    @Override
    public void endRow(int rowNum) {
        if (!headerRowDetected) {
            if (RowProcessorUtil.isHeaderRow(currentRowMap, excelToInternalMap)) {
                for (Map.Entry<String, Object> entry : currentRowMap.entrySet()) {
                    int colIndex = Integer.parseInt(entry.getKey());
                    String rawHeader = entry.getValue().toString().trim().toLowerCase();
                    columnIndexToHeaderMap.put(colIndex, rawHeader);
                }
                headerRowDetected = true;
                System.out.println("✅ Header Row Detected with Mapping: " + columnIndexToHeaderMap);
            }
        } else {
            if (!ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
                InvoiceRecordDTO dto = RowProcessorUtil.mapToInvoiceDTO(expectedHeaders, columnIndexToHeaderMap, currentRowMap, excelToInternalMap);
                if (RowProcessorUtil.isInvoiceAndClaimEmpty(dto)) {
                    System.out.println("⏩ Skipping row " + rowNum + " because Invoice # and Claim # are empty.");
                    return;
                }
                records.add(dto);
            }
        }
    }

    @Override
    public void cell(String cellReference, String formattedValue, XSSFComment comment) {
        int colIndex = ExcelHeaderUtils.getColumnIndex(cellReference);
        currentRowMap.put(String.valueOf(colIndex), formattedValue != null ? formattedValue.trim() : "");
    }

    @Override
    public void headerFooter(String text, boolean isHeader, String tagName) {
        // No-op
    }
}
