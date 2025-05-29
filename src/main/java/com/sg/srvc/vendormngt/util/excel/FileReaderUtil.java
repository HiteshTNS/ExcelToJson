package com.sg.srvc.vendormngt.util.excel;

import com.sg.srvc.vendormngt.dto.ExcelRecordRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.ExcelResponseDTO;
import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import com.sg.srvc.vendormngt.exception.CustomException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.ZipException;

public class FileReaderUtil {

    private static final int THREAD_THRESHOLD = 1000;
    private static final int BATCH_SIZE = 2500;

    public static ExcelResponseDTO readAndConvert(String filePath) {
        List<InvoiceRecordDTO> allRecords;

        if (filePath.toLowerCase().endsWith(".csv")) {
            allRecords = CsvProcessor.parseCsv(filePath);
        } else if (filePath.toLowerCase().endsWith(".xlsx")) {
            allRecords = readXlsx(filePath);
        } else {
            throw new CustomException("Unsupported file format. Only .xlsx and .csv allowed.");
        }


        List<ExcelRecordRequestDTO> batches = (allRecords.size() > THREAD_THRESHOLD)
                ? processInParallel(allRecords)
                : processSequentially(allRecords);

        ExcelResponseDTO response = new ExcelResponseDTO();
        response.setRecordDetails(batches);
        ExcelRequestDTO requestDTO = new ExcelRequestDTO();
        try {
            String path = JsonWriter.save(response,
                    "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel",
                    "invoice_data");
            System.out.println("✅ JSON saved at: " + path);
        } catch (IOException e) {
            throw new CustomException("Failed to save output: " + e.getMessage());
        }

        return response;
    }

    private static List<InvoiceRecordDTO> readXlsx(String filePath) {
        List<InvoiceRecordDTO> allRecords = new ArrayList<>();

//        ZipSecureFile.setMinInflateRatio(0.005); // to prevent zip bomb issues, optional

        try (OPCPackage pkg = OPCPackage.open(new File(filePath), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            DataFormatter formatter = new DataFormatter();
            SheetProcessor handler = new SheetProcessor(allRecords);

            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    XMLReader parser = org.xml.sax.helpers.XMLReaderFactory.createXMLReader();
                    parser.setContentHandler(new org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler(
                            reader.getStylesTable(), null, reader.getSharedStringsTable(), handler, formatter, false));
                    parser.parse(new InputSource(sheetStream));
                }
            }

        } catch (ZipException e) {
            throw new CustomException("Invalid file format. Ensure the file is a valid .xlsx file.");
        } catch (Exception e) {
            throw new CustomException("Error processing Excel file: " + e.getMessage());
        }

        return allRecords;
    }

    private static List<ExcelRecordRequestDTO> processSequentially(List<InvoiceRecordDTO> allRecords) {
        List<ExcelRecordRequestDTO> batches = new ArrayList<>();
        int totalRecords = allRecords.size();

        for (int i = 0; i < totalRecords; i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, totalRecords);
            List<InvoiceRecordDTO> subList = allRecords.subList(i, end);

            ExcelRecordRequestDTO batch = new ExcelRecordRequestDTO();
            batch.setInvoiceFileRecords(new ArrayList<>(subList));
            batch.setInvoiceFileMasterId(ThreadLocalRandom.current().nextLong(100000, 999999));
            batches.add(batch);
        }
        return batches;
    }

    private static List<ExcelRecordRequestDTO> processInParallel(List<InvoiceRecordDTO> allRecords) {
        int totalRecords = allRecords.size();
        int numBatches = (totalRecords + BATCH_SIZE - 1) / BATCH_SIZE;

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<ExcelRecordRequestDTO>> futures = new ArrayList<>();

        for (int i = 0; i < totalRecords; i += BATCH_SIZE) {
            int start = i;
            int end = Math.min(i + BATCH_SIZE, totalRecords);
            List<InvoiceRecordDTO> batchList = new ArrayList<>(allRecords.subList(start, end));

            futures.add(executor.submit(() -> {
                ExcelRecordRequestDTO batch = new ExcelRecordRequestDTO();
                batch.setInvoiceFileRecords(batchList);
                batch.setInvoiceFileMasterId(ThreadLocalRandom.current().nextLong(100000, 999999));
                return batch;
            }));
        }

        List<ExcelRecordRequestDTO> batches = new ArrayList<>();
        try {
            for (Future<ExcelRecordRequestDTO> future : futures) {
                batches.add(future.get());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomException("Error during parallel processing: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        return batches;
    }
}
