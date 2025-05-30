package com.sg.srvc.vendormngt.dto;

import lombok.Data;

import java.util.Map;

@Data
public class Column {
    private String excelHeader;
    private String internalName;
    private String type;  // "string", "date", "numeric"
    private boolean required;
    private Map<String, Object> constraints; // maxLength, format, min, etc.
}