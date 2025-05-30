package com.sg.srvc.vendormngt.util.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sg.srvc.vendormngt.dto.Column;
import com.sg.srvc.vendormngt.dto.VendorHeaderMapping;
import com.sg.srvc.vendormngt.exception.CustomException;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ExcelHeaderUtils {

    public static final List<String> HEADER_KEYWORDS = Arrays.asList("Company Name","invoice", "contract", "claim", "insured", "date", "vin", "amount");

    public static Map<String, String> loadHeaderMapping(String vendorCode) {
        InputStream is = ExcelHeaderUtils.class.getClassLoader().getResourceAsStream("configs/" + vendorCode + ".json");

        if (is == null) {
            throw new CustomException("Header mapping file not found for vendor: " + vendorCode);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            VendorHeaderMapping mapping = mapper.readValue(is, VendorHeaderMapping.class);

            Map<String, String> headerMap = new HashMap<>();
            for (Column col : mapping.getColumns()) {
                headerMap.put(col.getExcelHeader().toLowerCase(), col.getInternalName());
            }
            return headerMap;
        } catch (IOException e) {
            throw new CustomException("Failed to read header mapping file for vendor: " + vendorCode);
        }
    }

    // Map internal field names to required-flag (Boolean)
    public static Map<String, Boolean> loadValidationRules(String vendorCode) {
        String fileName = "configs/" + vendorCode + ".json";
        InputStream is = ExcelHeaderUtils.class.getClassLoader().getResourceAsStream(fileName);

        if (is == null) {
            throw new CustomException("Mapping file not found for vendor: " + vendorCode);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            VendorHeaderMapping mapping = mapper.readValue(is, VendorHeaderMapping.class);

            Map<String, Boolean> validationMap = new HashMap<>();
            for (Column col : mapping.getColumns()) {
                validationMap.put(col.getInternalName(), col.isRequired());
            }
            return validationMap;
        } catch (IOException e) {
            throw new CustomException("Failed to read header mapping file for vendor: " + vendorCode);
        }
    }

    public static List<Column> loadFullValidationRules(String vendorCode) {
        String fileName = "configs/" + vendorCode + ".json";
        InputStream is = ExcelHeaderUtils.class.getClassLoader().getResourceAsStream(fileName);

        if (is == null) {
            throw new CustomException("Mapping file not found for vendor: " + vendorCode);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            VendorHeaderMapping mapping = mapper.readValue(is, VendorHeaderMapping.class);
            return mapping.getColumns(); // Return full Column list
        } catch (IOException e) {
            throw new CustomException("Failed to read header mapping file for vendor: " + vendorCode);
        }
    }


    public static boolean isEmptyRow(Map<String, Object> row) {
        return row.values().stream().allMatch(val -> val == null || val.toString().trim().isEmpty());
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
