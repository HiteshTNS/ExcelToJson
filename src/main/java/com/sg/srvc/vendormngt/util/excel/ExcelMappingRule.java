package com.sg.srvc.vendormngt.util.excel;

import java.util.Map;

public class ExcelMappingRule {
    private Map<String, String> headerMappings;

    public Map<String, String> getHeaderMappings() {
        return headerMappings;
    }

    public void setHeaderMappings(Map<String, String> headerMappings) {
        this.headerMappings = headerMappings;
    }
}
