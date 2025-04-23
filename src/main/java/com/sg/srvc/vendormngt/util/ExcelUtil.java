package com.sg.srvc.vendormngt.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipException;

public class ExcelUtil {

    private static final int BATCH_SIZE = 300;
    private static final int THREAD_THRESHOLD = 500;
    public static final List<String> HEADER_KEYWORDS = Arrays.asList("invoice", "contract", "claim", "insured", "date", "vin", "amount");

    public static ExcelResponseDTO readAndConvert(String filePath) {
        List<InvoiceRecordDTO> allRecords = new ArrayList<>();

        try (OPCPackage pkg = OPCPackage.open(new File(filePath), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            // ✅ Correct
            SharedStrings sst = reader.getSharedStringsTable();
            StylesTable styles = reader.getStylesTable();

            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    processSheet(styles, sst, sheetStream, allRecords);
                }
            }

        } catch (ZipException e) {
            throw new CustomException("Invalid file format. Ensure the file is a valid .xlsx file.");
        } catch (Exception e) {
            throw new CustomException("Error processing Excel: " + e.getMessage());
        }

        int total = allRecords.size();

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

        List<ExcelRecordRequestDTO> batches;
        try {
            if (total > THREAD_THRESHOLD) {
                batches = processRecordsInParallel(allRecords);
            } else {
                batches = processRecordsSequentially(allRecords);
            }
        } catch (Exception e) {
            throw new CustomException("Batching error: " + e.getMessage());
        }

        ExcelResponseDTO response = new ExcelResponseDTO();
        response.setMetadata(meta);
        response.setRecordDetails(batches);

        try {
            String path = saveJsonToFile(response, "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel", "invoice_data");
            System.out.println("✅ JSON saved at: " + path);
        } catch (IOException e) {
            throw new CustomException("Failed to save output: " + e.getMessage());
        }

        return response;
    }

    private static void processSheet(StylesTable styles, SharedStrings sst, InputStream sheetStream, List<InvoiceRecordDTO> allRecords)
            throws IOException, SAXException, ParserConfigurationException {

        XMLReader parser = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
        DataFormatter formatter = new DataFormatter();
        SheetHandler handler = new SheetHandler(allRecords);
        ContentHandler contentHandler = new org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler(
                styles, null, sst, handler, formatter, false);
        parser.setContentHandler(contentHandler);
        parser.parse(new InputSource(sheetStream));
    }

    private static List<ExcelRecordRequestDTO> processRecordsSequentially(List<InvoiceRecordDTO> allRecords) {
        List<ExcelRecordRequestDTO> batches = new ArrayList<>();
        for (int i = 0; i < allRecords.size(); i += BATCH_SIZE) {
            List<InvoiceRecordDTO> batch = allRecords.subList(i, Math.min(i + BATCH_SIZE, allRecords.size()));
            ExcelRecordRequestDTO dto = new ExcelRecordRequestDTO();
            dto.setInvoiceFileMasterId(new Random().nextLong(100000, 999999));
            dto.setInvoiceFileRecords(batch);
            batches.add(dto);
        }
        return batches;
    }

    private static List<ExcelRecordRequestDTO> processRecordsInParallel(List<InvoiceRecordDTO> allRecords) throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        List<Callable<List<ExcelRecordRequestDTO>>> tasks = new ArrayList<>();
        List<ExcelRecordRequestDTO> batches = new ArrayList<>();

        for (int i = 0; i < allRecords.size(); i += BATCH_SIZE) {
            final int start = i;
            final int end = Math.min(i + BATCH_SIZE, allRecords.size());
            tasks.add(() -> {
                List<InvoiceRecordDTO> batch = allRecords.subList(start, end);
                ExcelRecordRequestDTO dto = new ExcelRecordRequestDTO();
                dto.setInvoiceFileMasterId(new Random().nextLong(100000, 999999));
                dto.setInvoiceFileRecords(batch);
                return Collections.singletonList(dto);
            });
        }

        List<Future<List<ExcelRecordRequestDTO>>> futures = executorService.invokeAll(tasks);
        for (Future<List<ExcelRecordRequestDTO>> future : futures) {
            batches.addAll(future.get());
        }
        executorService.shutdown();
        return batches;
    }

    public static String saveJsonToFile(Object jsonObj, String outputDir, String filePrefix) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = filePrefix + "_" + timestamp + ".json";
        File file = new File(outputDir, fileName);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, jsonObj);
        return file.getAbsolutePath();
    }

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
}
