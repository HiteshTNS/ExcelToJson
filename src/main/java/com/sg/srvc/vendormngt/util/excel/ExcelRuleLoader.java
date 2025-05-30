package com.sg.srvc.vendormngt.util.excel;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class ExcelRuleLoader {
    public static ExcelMappingRule loadRule(String vendorCode) throws Exception {
        String filePath = "config/" + vendorCode + ".json";
        InputStream is = ExcelRuleLoader.class.getClassLoader().getResourceAsStream(filePath);

        if (is == null) {
            throw new RuntimeException("Rule file not found for vendor: " + vendorCode);
        }

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(is, ExcelMappingRule.class);
    }
}
