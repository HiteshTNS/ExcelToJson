package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SheetProcessor implements XSSFSheetXMLHandler.SheetContentsHandler {

    private final List<InvoiceRecordDTO> records;
    private final List<String> headers = new ArrayList<>();
    private final String vendorCode;
    private final Map<String, String> excelToInternalMap;
    private Map<String, Object> currentRowMap;
    private boolean headerRowDetected = false;

    public SheetProcessor(List<InvoiceRecordDTO> records, String vendorCode) {
        this.records = records;
        this.vendorCode = vendorCode;
        this.excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
    }

    @Override
    public void startRow(int rowNum) {
        currentRowMap = new LinkedHashMap<>();
    }

    @Override
    public void endRow(int rowNum) {
        if (!headerRowDetected) {
            if (RowProcessorUtil.isHeaderRow(currentRowMap, excelToInternalMap)) {
                currentRowMap.values().forEach(val -> headers.add(val.toString()));
                headerRowDetected = true;

                System.out.println("✅ Header Row Detected:");
                headers.forEach(h -> System.out.println("Header: " + h));
            }
        } else if (!headers.isEmpty() && !ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
            InvoiceRecordDTO dto = RowProcessorUtil.mapToInvoiceDTO(headers, currentRowMap, excelToInternalMap);
            if (RowProcessorUtil.isInvoiceAndClaimEmpty(dto)) {
                System.out.println("⏩ Skipping row " + rowNum + " because Invoice # and Claim # are empty.");
                return;  // Skip adding this row, but continue processing others
            }
            records.add(dto);
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
