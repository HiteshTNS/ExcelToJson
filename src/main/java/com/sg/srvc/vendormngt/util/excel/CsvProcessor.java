package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CsvProcessor {

    public static List<InvoiceRecordDTO> parseCsv(String filePath) {
        List<InvoiceRecordDTO> records = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        boolean headerDetected = false;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1); // -1 to include trailing empty strings
                List<String> rowValues = new ArrayList<>();
                for (String val : values) {
                    rowValues.add(val.trim());
                }

                if (!headerDetected) {
                    // Count how many header keywords appear in this row (case-insensitive)
                    long match = rowValues.stream()
                            .filter(val -> !val.isEmpty())
                            .map(String::toLowerCase)
                            .filter(val -> ExcelHeaderUtils.HEADER_KEYWORDS.stream().anyMatch(val::contains))
                            .count();

                    if (match >= 3) {
                        // This row is header
                        for (String rawHeader : rowValues) {
                            headers.add(ExcelHeaderUtils.mapHeader(rawHeader));
                        }
                        headerDetected = true;

                        if (headers.size() != ExcelHeaderUtils.HEADER_KEYWORDS.size()) {
                            throw new RuntimeException("Header validation failed: Expected "
                                    + ExcelHeaderUtils.HEADER_KEYWORDS.size()
                                    + " headers but found " + headers.size());
                        }
                        continue; // skip header row from data
                    }
                } else {
                    // Data rows after header

                    // Skip empty row
                    boolean isEmpty = rowValues.stream().allMatch(String::isEmpty);
                    if (isEmpty) continue;

                    // Check all header columns have non-empty values
                    boolean hasAllData = true;
                    for (int i = 0; i < headers.size(); i++) {
                        if (i >= rowValues.size() || rowValues.get(i).isEmpty()) {
                            hasAllData = false;
                            break;
                        }
                    }
                    if (!hasAllData) continue;

                    Map<String, Object> request = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String key = headers.get(i);
                        Object value = (i < rowValues.size()) ? rowValues.get(i) : "";
                        request.put(key, value);
//                        System.out.println("Key : " + key);
                    }

                    InvoiceRecordDTO dto = new InvoiceRecordDTO();
                    dto.setClaimNumber(String.valueOf(request.getOrDefault("claim", "")));
                    dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoice", "")));
                    dto.setRequest(request);
                    dto.setStatus("PENDING");
                    records.add(dto);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }

        return records;
    }
}
