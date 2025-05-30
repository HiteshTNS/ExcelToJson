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
            } catch (Exception ignored) {}
        }

        val = val.replaceAll("[^a-zA-Z0-9.\\-/ \\-]", "").strip();
        return val.isBlank() ? null : val;
    }

    public static InvoiceRecordDTO mapToInvoiceDTO(List<String> headers, Map<String, Object> currentRowMap,
                                                   Map<String, String> excelToInternalMap) {
        Map<String, Object> request = new LinkedHashMap<>();

        for (int i = 0; i < headers.size(); i++) {
            String rawHeader = headers.get(i).trim().toLowerCase();
            String internalKey = excelToInternalMap.getOrDefault(rawHeader, rawHeader);
            Object value = currentRowMap.getOrDefault(String.valueOf(i), null);
            Object cleanedValue = cleanValue(value);
            request.put(internalKey, cleanedValue);
        }

        InvoiceRecordDTO dto = new InvoiceRecordDTO();
        dto.setClaimNumber(String.valueOf(request.getOrDefault("claimNumber", "")));
        dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoiceNumber", "")));
        dto.setRecJson(request);
        return dto;
    }

    public static void validateRecords(List<InvoiceRecordDTO> records, String vendorCode) {
        LocalDateTime now = LocalDateTime.now();
        String createdBy = "John Doe";

        List<Column> validationRules = ExcelHeaderUtils.loadFullValidationRules(vendorCode);

        for (InvoiceRecordDTO record : records) {
            record.setCreatedDate(now);
            record.setCreatedBy(createdBy);

            Map<String, Object> request = record.getRecJson();
            List<String> errors = new ArrayList<>();

            for (Column col : validationRules) {
                String key = col.getInternalName();
                Object value = request.get(key);

                if (col.isRequired() && (value == null || value.toString().isBlank())) {
                    errors.add(key + " is required");
                    request.put(key, null);
                    continue;
                }

                if (value == null || value.toString().isBlank()) continue;

                String type = col.getType();
                Map<String, Object> constraints = col.getConstraints();

                boolean valid = true;
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
                        valid = false;
                        errorMsg = e.getMessage() != null ? e.getMessage() : "invalid numeric format";
                    }
                } else if ("string".equalsIgnoreCase(type)) {
                    String str = value.toString();
                    if (constraints != null) {
                        if (constraints.containsKey("minLength") && str.length() < (int) constraints.get("minLength"))
                            errorMsg = "length < " + constraints.get("minLength");
                        else if (constraints.containsKey("maxLength") && str.length() > (int) constraints.get("maxLength"))
                            errorMsg = "length > " + constraints.get("maxLength");
                    }
                    valid = errorMsg.isEmpty();
                }

                if (!valid) {
                    errors.add(key + ": " + errorMsg);
                    request.put(key, null);
                }
            }

            if (errors.isEmpty()) {
                record.setStatus("VALID");
                record.setStatusDescription("Validated successfully");
            } else {
                record.setStatus("VALIDATION_FAILED");
                record.setStatusDescription(String.join("; ", errors));
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
