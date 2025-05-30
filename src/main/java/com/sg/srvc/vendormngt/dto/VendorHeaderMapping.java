package com.sg.srvc.vendormngt.dto;

import lombok.Data;

import java.util.List;

@Data
public class VendorHeaderMapping {
    private String vendorCode;
    private List<Column> columns;
}