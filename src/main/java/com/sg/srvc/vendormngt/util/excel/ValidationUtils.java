package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.ValidationColumnDTO;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ValidationUtils {

    public static String validateField(Object value, ValidationColumnDTO col) {
        String val = value != null ? value.toString().trim() : "";
        if (col.isRequired() && val.isEmpty()) {
            return col.getExcelHeader() + " is required but found empty";
        }

        if (!val.isEmpty()) {
            switch (col.getType()) {
                case "string":
                    if (col.getConstraints().containsKey("maxLength")) {
                        int max = (int) col.getConstraints().get("maxLength");
                        if (val.length() > max) {
                            return col.getExcelHeader() + " exceeds max length of " + max;
                        }
                    }
                    break;
                case "numeric":
                    try {
                        double num = Double.parseDouble(val);
                        if (col.getConstraints().containsKey("min")) {
                            double min = Double.parseDouble(col.getConstraints().get("min").toString());
                            if (num < min) return col.getExcelHeader() + " must be >= " + min;
                        }
                    } catch (NumberFormatException e) {
                        return col.getExcelHeader() + " should be a valid number";
                    }
                    break;
                case "date":
                    try {
                        String format = (String) col.getConstraints().get("format");
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
                        LocalDate.parse(val, dtf);
                    } catch (Exception e) {
                        return col.getExcelHeader() + " should match format " + col.getConstraints().get("format");
                    }
                    break;
            }
        }

        return null;
    }
}
