package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CsvProcessor {

    private final String vendorCode;
    private final Map<String, String> excelToInternalMap;
    private final List<String> expectedHeaders;
    private final Map<Integer, String> columnIndexToHeaderMap = new HashMap<>();
    private boolean headerRowDetected = false;

    public CsvProcessor(String vendorCode) {
        this.vendorCode = vendorCode;
        this.excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
        this.expectedHeaders = new ArrayList<>(excelToInternalMap.keySet());
    }

    public List<InvoiceRecordDTO> parseCsv(String filePath) throws IOException {
        List<InvoiceRecordDTO> records = new ArrayList<>();
        Set<String> detectedHeaderSet = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] cells = line.split(",", -1);

                List<String> trimmedCells = new ArrayList<>();
                for (String c : cells) {
                    trimmedCells.add(c.trim());
                }

                if (!headerRowDetected) {
                    Map<String, Object> tempRowMap = new HashMap<>();
                    for (int i = 0; i < trimmedCells.size(); i++) {
                        tempRowMap.put(String.valueOf(i), trimmedCells.get(i).toLowerCase());
                    }

                    if (RowProcessorUtil.isHeaderRow(tempRowMap, excelToInternalMap)) {
                        columnIndexToHeaderMap.clear();
                        for (int i = 0; i < trimmedCells.size(); i++) {
                            String header = trimmedCells.get(i).toLowerCase().trim();
                            if (header.isEmpty()) {
                                header = expectedHeaders.size() > i ? expectedHeaders.get(i).toLowerCase() : "column_" + i;
                            }
                            columnIndexToHeaderMap.put(i, header);
                            detectedHeaderSet.add(header);
                        }
                        headerRowDetected = true;

                        // Validate missing headers at header detection time
                        List<String> missingHeaders = new ArrayList<>();
                        for (String expected : expectedHeaders) {
                            if (!detectedHeaderSet.contains(expected.toLowerCase())) {
                                missingHeaders.add(expected);
                            }
                        }

                        if (!missingHeaders.isEmpty()) {
                            System.out.println("Missing Headers: " + String.join(", ", missingHeaders));
                        } else {
                            System.out.println("All expected headers are present.");
                        }

                        System.out.println("Header Row Detected with Mapping: " + columnIndexToHeaderMap);
                        continue;
                    } else {
                        continue;
                    }
                }

                Map<String, Object> currentRowMap = new HashMap<>();
                for (int i = 0; i < columnIndexToHeaderMap.size(); i++) {
                    String val = i < trimmedCells.size() ? trimmedCells.get(i) : null;
                    currentRowMap.put(String.valueOf(i), (val == null || val.isEmpty()) ? null : val);
                }

                if (ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
                    continue;
                }

                InvoiceRecordDTO dto = RowProcessorUtil.mapToInvoiceDTO(expectedHeaders, columnIndexToHeaderMap, currentRowMap, excelToInternalMap);

                // Extra check if header was missing
                if (!headerRowDetected || columnIndexToHeaderMap.isEmpty()) {
                    dto.setStatus("VALIDATION_FAILED");
                    dto.setStatusDescription("Header row not detected or invalid.");
                }

                records.add(dto);
            }
        }

        return records;
    }
}
