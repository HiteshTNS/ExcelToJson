// File: ValidationUtils.java
package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import com.sg.srvc.vendormngt.dto.Column;

import java.time.LocalDateTime;
import java.util.*;

public class ValidationUtils {

    public static void validateRecords(List<InvoiceRecordDTO> records, String vendorCode) {
        LocalDateTime now = LocalDateTime.now();
        String createdBy = "John Doe"; // Or inject/pull dynamically

        List<Column> validationRules = ExcelHeaderUtils.loadFullValidationRules(vendorCode);

        for (InvoiceRecordDTO record : records) {
            record.setCreatedDate(now);
            record.setCreatedBy(createdBy);

            Map<String, Object> request = record.getRecJson();
            List<String> errors = new ArrayList<>();

            for (Column col : validationRules) {
                String key = col.getInternalName();
                Object value = request.get(key);

                if (col.isRequired()) {
                    if (value == null || value.toString().isBlank()) {
                        errors.add(key + " is required");
                        request.put(key, null);
                        continue;
                    }
                }

                if (value == null || value.toString().isBlank()) continue;

                boolean valid = true;
                String errorMsg = "";
                String type = col.getType();
                Map<String, Object> constraints = col.getConstraints();

                if ("numeric".equalsIgnoreCase(type)) {
                    try {
                        double num = Double.parseDouble(value.toString());

                        if (constraints != null) {
                            if (constraints.containsKey("min")) {
                                double min = Double.parseDouble(constraints.get("min").toString());
                                if (num < min) {
                                    valid = false;
                                    errorMsg = "must be >= " + min;
                                }
                            }
                            if (valid && constraints.containsKey("max")) {
                                double max = Double.parseDouble(constraints.get("max").toString());
                                if (num > max) {
                                    valid = false;
                                    errorMsg = "must be <= " + max;
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        valid = false;
                        errorMsg = "invalid numeric format";
                    }
                } else if ("string".equalsIgnoreCase(type)) {
                    String str = value.toString();
                    if (constraints != null) {
                        if (constraints.containsKey("minLength")) {
                            int min = (int) constraints.get("minLength");
                            if (str.length() < min) {
                                valid = false;
                                errorMsg = "length < " + min;
                            }
                        }
                        if (valid && constraints.containsKey("maxLength")) {
                            int max = (int) constraints.get("maxLength");
                            if (str.length() > max) {
                                valid = false;
                                errorMsg = "length > " + max;
                            }
                        }
                    }
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
}
