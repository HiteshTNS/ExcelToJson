package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.ExcelMetadataResponseDTO;
import com.sg.srvc.vendormngt.dto.ExcelRecordRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;
import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import com.sg.srvc.vendormngt.exception.CustomException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.ZipException;

public class ExcelReader {

    private static final int BATCH_SIZE = 300;
    private static final int THREAD_THRESHOLD = 500;

    public static ExcelResponseDTO readAndConvert(String filePath) {
        List<InvoiceRecordDTO> allRecords = new ArrayList<>();
        //Opens the file in readonly format
        try (OPCPackage pkg = OPCPackage.open(new File(filePath), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg); //reads data without loading entier sheet into memory
            SharedStrings sst = reader.getSharedStringsTable(); //stores repeated strings in one table
            StylesTable styles = reader.getStylesTable(); //format date and currency

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            DataFormatter formatter = new DataFormatter();
            SheetProcessor handler = new SheetProcessor(allRecords);
            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    XMLReader parser = org.xml.sax.helpers.XMLReaderFactory.createXMLReader(); //create SAX Parser
                    parser.setContentHandler(new org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler(
                            styles, null, sst, handler, formatter, false));
                    parser.parse(new InputSource(sheetStream)); //Parsing -> will trgger sheet handler
                }
            }

        } catch (ZipException e) {
            throw new CustomException("Invalid file format. Ensure the file is a valid .xlsx file.");
        } catch (Exception e) {
            throw new CustomException("Error processing Excel: " + e.getMessage());
        }

        ExcelMetadataResponseDTO meta = createMetadata(filePath, allRecords.size());

        List<ExcelRecordRequestDTO> batches = (allRecords.size() > THREAD_THRESHOLD)
                ? processInParallel(allRecords)
                : processSequentially(allRecords);

        ExcelResponseDTO response = new ExcelResponseDTO();
        response.setMetadata(meta);
        response.setRecordDetails(batches);

        try {
            String path = JsonWriter.save(response, "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel", "invoice_data");
            System.out.println("✅ JSON saved at: " + path);
        } catch (IOException e) {
            throw new CustomException("Failed to save output: " + e.getMessage());
        }

        return response;
    }

    private static ExcelMetadataResponseDTO createMetadata(String filePath, int total) {
        ExcelMetadataResponseDTO meta = new ExcelMetadataResponseDTO();
        meta.setCorrelationId(UUID.randomUUID().toString());
        meta.setVendorCode("xyz");
        meta.setName(new File(filePath).getName());
        meta.setFileExtension("xlsx");
        meta.setTotalRecCount(total);
        meta.setSuccessRecCount(total);
        meta.setPendingRecCount(0);
        meta.setErrorRecCount(0);
        meta.setStatus("PENDING");
        meta.setStatusDescription("PENDING");
        meta.setCreatedBy("xyz");
        meta.setUrl("http://dummy-url.com/file/" + meta.getCorrelationId());
        return meta;
    }

    private static List<ExcelRecordRequestDTO> processSequentially(List<InvoiceRecordDTO> records) {
        List<ExcelRecordRequestDTO> result = new ArrayList<>();
        for (int i = 0; i < records.size(); i += BATCH_SIZE) {
            List<InvoiceRecordDTO> batch = records.subList(i, Math.min(i + BATCH_SIZE, records.size()));
            ExcelRecordRequestDTO dto = new ExcelRecordRequestDTO();
            dto.setInvoiceFileMasterId(new Random().nextLong(100000, 999999));
            dto.setInvoiceFileRecords(batch);
            result.add(dto);
        }
        return result;
    }

    private static List<ExcelRecordRequestDTO> processInParallel(List<InvoiceRecordDTO> records) {
        List<ExcelRecordRequestDTO> result = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        List<Future<ExcelRecordRequestDTO>> futures = new ArrayList<>();

        for (int i = 0; i < records.size(); i += BATCH_SIZE) {
            final int start = i;
            final int end = Math.min(i + BATCH_SIZE, records.size());
            futures.add(executor.submit(() -> {
                ExcelRecordRequestDTO dto = new ExcelRecordRequestDTO();
                dto.setInvoiceFileMasterId(new Random().nextLong(100000, 999999));
                dto.setInvoiceFileRecords(records.subList(start, end));
                return dto;
            }));
        }

        for (Future<ExcelRecordRequestDTO> f : futures) {
            try {
                result.add(f.get());
            } catch (Exception e) {
                throw new CustomException("Error processing batch: " + e.getMessage());
            }
        }

        executor.shutdown();
        return result;
    }
}
