package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.Column;
import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

import java.time.LocalDateTime;
import java.util.*;

public class SheetProcessor implements XSSFSheetXMLHandler.SheetContentsHandler {

    private final List<InvoiceRecordDTO> records;
    private final List<String> headers = new ArrayList<>();
    private final String vendorCode;
    private final Map<String, String> excelToInternalMap;
    private final Map<String, Boolean> requiredFields;
    private Map<String, Object> currentRowMap;
    private boolean headerRowDetected = false;

    public SheetProcessor(List<InvoiceRecordDTO> records, String vendorCode) {
        this.records = records;
        this.vendorCode = vendorCode;
        this.excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
        this.requiredFields = ExcelHeaderUtils.loadValidationRules(vendorCode);
//        loadVendorHeaderMapping(vendorCode);
    }

    public static Map<String, Boolean> getRequiredFieldsForVendor(String vendorCode) {
        return ExcelHeaderUtils.loadValidationRules(vendorCode);
    }



    @Override
    public void startRow(int rowNum) {
        currentRowMap = new LinkedHashMap<>();
    }

    @Override
    public void endRow(int rowNum) {
        if (!headerRowDetected) {
            long matchCount = currentRowMap.values().stream()
                    .filter(val -> val instanceof String)
                    .map(val -> ((String) val).toLowerCase())
                    .filter(excelToInternalMap::containsKey)
                    .count();

            if (matchCount >= 3) {
                currentRowMap.values().forEach(val -> headers.add(val.toString()));
                headerRowDetected = true;

                System.out.println("✅ Header Row Detected:");
                headers.forEach(h -> System.out.println("Header: " + h));
            }

        } else if (!headers.isEmpty() && !ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
            List<String> errorMessages = new ArrayList<>();
            Map<String, Object> request = new LinkedHashMap<>();

            for (int i = 0; i < headers.size(); i++) {
                String rawHeader = headers.get(i).trim().toLowerCase();
                String internalKey = excelToInternalMap.getOrDefault(rawHeader, rawHeader);
                Object value = currentRowMap.getOrDefault(String.valueOf(i), null);
                Object cleanedValue = cleanValue(value); //cleaning and formating
                request.put(internalKey, cleanedValue);
            }

            // **Put your summary row check here:**
            boolean isSummaryRow = request.values().stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .anyMatch(val -> val.trim().equalsIgnoreCase("total")
                            || val.trim().equalsIgnoreCase("subtotal")
                            || val.trim().equalsIgnoreCase("summary")
                            || val.trim().equalsIgnoreCase("grand total")
                            || val.trim().equalsIgnoreCase("balance"));

            if (isSummaryRow) {
                // Skip this row
                return;
            }

            InvoiceRecordDTO dto = new InvoiceRecordDTO();
            dto.setClaimNumber(String.valueOf(request.getOrDefault("claimNumber", "")));
            dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoiceNumber", "")));
            dto.setRecJson(request);
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

    public static void validateRecords(List<InvoiceRecordDTO> records, String vendorCode) {
        LocalDateTime now = LocalDateTime.now();
        String createdBy = "John Doe"; // You can pass it as a parameter too

        List<Column> validationRules = ExcelHeaderUtils.loadFullValidationRules(vendorCode);

        for (InvoiceRecordDTO record : records) {
            record.setCreatedDate(now);
            record.setCreatedBy(createdBy);

            Map<String, Object> request = record.getRecJson();
            List<String> errors = new ArrayList<>();

            for (Column col : validationRules) {
                String key = col.getInternalName();
                Object value = request.get(key);

                // Check required
                if (col.isRequired()) {
                    if (value == null || value.toString().isBlank()) {
                        errors.add(key + " is required");
                        request.put(key, null);
                        continue;
                    }
                }

                // Skip further checks if value is null or blank
                if (value == null || value.toString().isBlank()) {
                    continue;
                }

                // Check type and constraints
                String type = col.getType();
                Map<String, Object> constraints = col.getConstraints();

                boolean valid = true;
                String errorMsg = "";

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



    private Object cleanValue(Object value) {
        if (value == null) return null;

        String val = value.toString().trim();
        val = val.strip();
        // Try parsing as date using common date patterns
        List<String> datePatterns = Arrays.asList(
                "M/d/yy", "MM/dd/yy", "M/d/yyyy", "MM/dd/yyyy",
                "yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy"
        );

        for (String pattern : datePatterns) {
            try {
                java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat(pattern);
                inputFormat.setLenient(false);
                Date parsedDate = inputFormat.parse(val);
                java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
                return outputFormat.format(parsedDate);
            } catch (Exception e) {
                // Not a date, continue
            }
        }

        // Remove special characters except letters, digits, dot, slash, dash, space, and minus sign
        // This keeps things like addresses, codes, etc., cleaner
        val = val.replaceAll("[^a-zA-Z0-9.\\-/ \\-]", "");
        val = val.strip();
        // If after cleaning the string is empty, return null
        if (val.isBlank()) return null;
        return val;
    }



}
