package com.sg.srvc.vendormngt.util.excel;

import java.util.*;

public class ExcelHeaderUtils {

    public static final List<String> HEADER_KEYWORDS = Arrays.asList("Company Name","invoice", "contract", "claim", "insured", "date", "vin", "amount");

    public static String mapHeader(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) return "";
        String cleaned = rawHeader.replaceAll("[^a-zA-Z0-9\\s_]", "");
        String[] parts = cleaned.trim().toLowerCase().split("[\\s_]+");
        if (parts.length == 0) return "";
        StringBuilder result = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result.append(parts[i].substring(0, 1).toUpperCase()).append(parts[i].substring(1));
        }
        return result.toString();
    }

    public static boolean isEmptyRow(Map<String, Object> row) {
        return row.values().stream().allMatch(val -> val == null || val.toString().trim().isEmpty());
    }

    public static boolean isTotalRow(Map<String, Object> row) {
        for (Object value : row.values()) {
            if (value != null && value.toString().toLowerCase().contains("total")) {
                return true;
            }
        }
        return false;
    }

    public static int getColumnIndex(String cellRef) {
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            char ch = cellRef.charAt(i);
            if (Character.isDigit(ch)) break;
            col = col * 26 + (ch - 'A' + 1);
        }
        return col - 1;
    }
}
