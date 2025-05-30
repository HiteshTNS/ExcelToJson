package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CsvProcessor {

    public static List<InvoiceRecordDTO> parseCsv(String filePath, String vendorCode) {
        List<InvoiceRecordDTO> records = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        boolean headerDetected = false;

        // Load vendor-specific mappings
        Map<String, String> excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
        Map<String, Boolean> requiredFields = ExcelHeaderUtils.loadValidationRules(vendorCode);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",", -1); // Preserve empty values
                List<String> rowValues = new ArrayList<>();
                for (String val : values) {
                    rowValues.add(val.trim());
                }

                if (!headerDetected) {
                    long match = rowValues.stream()
                            .filter(val -> !val.isEmpty())
                            .map(String::toLowerCase)
                            .filter(excelToInternalMap::containsKey)
                            .count();

                    if (match >= 3) {
                        headers.addAll(rowValues);
                        headerDetected = true;

                        System.out.println("✅ CSV Header Detected:");
                        headers.forEach(h -> System.out.println("Header: " + h));
                        continue;
                    }
                } else {
                    // Skip empty row
                    boolean isEmpty = rowValues.stream().allMatch(String::isEmpty);
                    if (isEmpty) continue;

                    Map<String, Object> request = new LinkedHashMap<>();
                    for (int i = 0; i < headers.size(); i++) {
                        String rawHeader = headers.get(i).trim().toLowerCase();
                        String internalKey = excelToInternalMap.getOrDefault(rawHeader, rawHeader);
                        Object value = (i < rowValues.size()) ? rowValues.get(i) : "";
                        request.put(internalKey, value);
                    }

                    // Validate required fields
                    boolean valid = true;
                    for (Map.Entry<String, Boolean> entry : requiredFields.entrySet()) {
                        String key = entry.getKey();
                        boolean isRequired = entry.getValue();
                        if (isRequired) {
                            Object val = request.get(key);
                            if (val == null || val.toString().isBlank()) {
                                valid = false;
                                break;
                            }
                        }
                    }
                    if (!valid) continue;

                    InvoiceRecordDTO dto = new InvoiceRecordDTO();
                    dto.setClaimNumber(String.valueOf(request.getOrDefault("claimNumber", "")));
                    dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoiceNumber", "")));
                    dto.setRecJson(request);
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
