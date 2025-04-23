//package com.sg.srvc.vendormngt.Processor;
//
//import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
//import org.apache.poi.ss.usermodel.Cell;
//import org.apache.poi.ss.usermodel.Row;
//import com.sg.srvc.vendormngt.util.ExcelUtil;
//import java.util.ArrayList;
//import java.util.LinkedHashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.Callable;
//
//public class RecordProcessorTask implements Callable<List<InvoiceRecordDTO>> {
//    private final List<Row> rows;
//    private final List<String> headers;
//    private final int invoiceIndex;
//    private final int claimIndex;
//
//    public RecordProcessorTask(List<Row> rows, List<String> headers, int invoiceIndex, int claimIndex) {
//        this.rows = rows;
//        this.headers = headers;
//        this.invoiceIndex = invoiceIndex;
//        this.claimIndex = claimIndex;
//    }
//
//    @Override
//    public List<InvoiceRecordDTO> call() {
//        List<InvoiceRecordDTO> records = new ArrayList<>();
//        for (Row row : rows) {
//            if (row == null) continue;
//
//            Map<String, Object> requestMap = new LinkedHashMap<>();
//            for (int j = 0; j < headers.size(); j++) {
//                Cell cell = row.getCell(j);
//                requestMap.put(ExcelUtil.mapHeader(headers.get(j)), ExcelUtil.getCellValue(cell));
//            }
//
//            InvoiceRecordDTO record = new InvoiceRecordDTO();
//            record.setRequest(requestMap);
//            record.setInvoiceNumber(invoiceIndex != -1 ? ExcelUtil.getCellValue(row.getCell(invoiceIndex)) : "");
//            record.setClaimNumber(claimIndex != -1 ? ExcelUtil.getCellValue(row.getCell(claimIndex)) : "");
//            record.setStatus("PENDING");
//
//            records.add(record);
//        }
//        return records;
//    }
//}
