package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.Column;
import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

public class RowProcessorUtil {

    public static boolean isHeaderRow(Map<String, Object> row, Map<String, String> excelToInternalMap) {
        long matchCount = row.values().stream()
                .filter(val -> val instanceof String)
                .map(val -> ((String) val).toLowerCase())
                .filter(excelToInternalMap::containsKey)
                .count();
        return matchCount >= 3;
    }

    public static Object cleanValue(Object value) {
        if (value == null) return null;

        String val = value.toString().trim();
        val = val.strip();

        List<String> datePatterns = Arrays.asList(
                "M/d/yy", "MM/dd/yy", "M/d/yyyy", "MM/dd/yyyy",
                "yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy"
        );

        for (String pattern : datePatterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern);
                inputFormat.setLenient(false);
                Date parsedDate = inputFormat.parse(val);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                return outputFormat.format(parsedDate);
            } catch (Exception ignored) {
            }
        }

        val = val.replaceAll("[^a-zA-Z0-9.\\-/ \\-]", "").strip();
        return val.isBlank() ? null : val;
    }

    public static InvoiceRecordDTO mapToInvoiceDTO(List<String> expectedHeaders,
                                                   Map<Integer, String> columnIndexToHeaderMap,
                                                   Map<String, Object> currentRowMap,
                                                   Map<String, String> excelToInternalMap) {

        Map<String, Object> request = new LinkedHashMap<>();
        List<String> missingHeaders = new ArrayList<>();

        Map<String, Integer> headerToColumnIndex = new HashMap<>();
        for (Map.Entry<Integer, String> entry : columnIndexToHeaderMap.entrySet()) {
            if (entry.getValue() != null) {
                headerToColumnIndex.put(entry.getValue().trim().toLowerCase(), entry.getKey());
            }
        }

        Set<Integer> availableFallbackIndices = new LinkedHashSet<>();
        for (int i = 0; i < currentRowMap.size(); i++) {
            if (!columnIndexToHeaderMap.containsKey(i)) {
                availableFallbackIndices.add(i);
            }
        }

        for (String expectedHeader : expectedHeaders) {
            String expectedLower = expectedHeader.toLowerCase();
            String internalKey = excelToInternalMap.getOrDefault(expectedLower, expectedLower);
            Object value = null;

            if (headerToColumnIndex.containsKey(expectedLower)) {
                Integer index = headerToColumnIndex.get(expectedLower);
                value = currentRowMap.get(String.valueOf(index));
            } else {
                missingHeaders.add(expectedHeader);
                Iterator<Integer> it = availableFallbackIndices.iterator();
                while (it.hasNext()) {
                    Integer idx = it.next();
                    Object possibleValue = currentRowMap.get(String.valueOf(idx));
                    if (possibleValue != null && !String.valueOf(possibleValue).isBlank()) {
                        value = possibleValue;
                        it.remove();
                        break;
                    }
                }
            }

            request.put(internalKey, cleanValue(value));
        }

        InvoiceRecordDTO dto = new InvoiceRecordDTO();
        dto.setClaimNumber(String.valueOf(request.getOrDefault("claimNumber", "")));
        dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoiceNumber", "")));
        dto.setRecJson(request);

        if (!missingHeaders.isEmpty()) {
            dto.setStatus("VALIDATION_FAILED");
            dto.setStatusDescription("Missing expected headers: " + String.join(", ", missingHeaders));
        }

        return dto;
    }

    public static void validateRecords(List<InvoiceRecordDTO> records, String vendorCode) {
        LocalDateTime now = LocalDateTime.now();
        String createdBy = "John Doe";

        List<Column> validationRules = ExcelHeaderUtils.loadFullValidationRules(vendorCode);

        for (InvoiceRecordDTO record : records) {
            record.setCreatedDate(now);
            record.setCreatedBy(createdBy);

            List<String> errors = new ArrayList<>();
            boolean isValid = true;

            Map<String, Object> request = record.getRecJson();

            for (Column col : validationRules) {
                String key = col.getInternalName();
                Object value = request.get(key);

                if (col.isRequired() && (value == null || value.toString().isBlank())) {
                    errors.add(key + " is required");
                    request.put(key, null);
                    isValid = false;
                    continue;
                }

                if (value == null || value.toString().isBlank()) continue;

                String type = col.getType();
                Map<String, Object> constraints = col.getConstraints();
                String errorMsg = "";

                if ("numeric".equalsIgnoreCase(type)) {
                    try {
                        double num = Double.parseDouble(value.toString());
                        if (constraints != null) {
                            if (constraints.containsKey("min") && num < Double.parseDouble(constraints.get("min").toString()))
                                throw new IllegalArgumentException("must be >= " + constraints.get("min"));
                            if (constraints.containsKey("max") && num > Double.parseDouble(constraints.get("max").toString()))
                                throw new IllegalArgumentException("must be <= " + constraints.get("max"));
                        }
                    } catch (Exception e) {
                        errorMsg = e.getMessage() != null ? e.getMessage() : "invalid numeric format";
                        isValid = false;
                    }
                } else if ("string".equalsIgnoreCase(type)) {
                    String str = value.toString();
                    if (constraints != null) {
                        if (constraints.containsKey("minLength") && str.length() < (int) constraints.get("minLength"))
                            errorMsg = "length < " + constraints.get("minLength");
                        else if (constraints.containsKey("maxLength") && str.length() > (int) constraints.get("maxLength"))
                            errorMsg = "length > " + constraints.get("maxLength");
                    }
                    if (!errorMsg.isEmpty()) isValid = false;
                }

                if (!errorMsg.isEmpty()) {
                    errors.add(key + ": " + errorMsg);
                    request.put(key, null);
                }
            }

            if (errors.isEmpty() && !"VALIDATION_FAILED".equals(record.getStatus())) {
                record.setStatus("VALID");
                record.setStatusDescription("Validated successfully");
            } else {
                record.setStatus("VALIDATION_FAILED");
                if (record.getStatusDescription() == null || record.getStatusDescription().isBlank()) {
                    record.setStatusDescription(String.join("; ", errors));
                } else {
                    record.setStatusDescription(record.getStatusDescription() + "; " + String.join("; ", errors));
                }
            }
        }
    }

    public static boolean isInvoiceAndClaimEmpty(InvoiceRecordDTO dto) {
        String invoice = dto.getInvoiceNumber();
        String claim = dto.getClaimNumber();
        return isNullOrEmptyOrLiteralNull(invoice) && isNullOrEmptyOrLiteralNull(claim);
    }

    static boolean isNullOrEmptyOrLiteralNull(String val) {
        return val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null");
    }
}
