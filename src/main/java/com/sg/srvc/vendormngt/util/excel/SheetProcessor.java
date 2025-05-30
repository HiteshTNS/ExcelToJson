package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;

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

            // Minimal validation for required fields
//            boolean valid = true;
            for (Map.Entry<String, Boolean> entry : requiredFields.entrySet()) {
                String key = entry.getKey();
                boolean isRequired = entry.getValue();

                if (isRequired) {
                    Object val = request.get(key);
                    if (val == null || val.toString().isBlank()) {
//                        valid = false;
                        errorMessages.add("Field '" + key + "' is required but missing.");
                        break;
                    }
                }
            }

//            if (!valid) return;

            InvoiceRecordDTO dto = new InvoiceRecordDTO();
            dto.setClaimNumber(String.valueOf(request.getOrDefault("claimNumber", "")));
            dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoiceNumber", "")));
            dto.setRecJson(request);
            if (!errorMessages.isEmpty()) {
                dto.setStatus("VALIDATION_FAILED");
                dto.setStatusDescription(String.valueOf(errorMessages));
            } else {
                dto.setStatus("VALID");
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
