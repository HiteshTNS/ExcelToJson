package com.sg.srvc.vendormngt.util.excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sg.srvc.vendormngt.dto.Column;
import com.sg.srvc.vendormngt.dto.InvoiceFileResponseDTO;
import com.sg.srvc.vendormngt.dto.InvoiceRecordDTO;
import com.sg.srvc.vendormngt.dto.VendorHeaderMapping;
import com.sg.srvc.vendormngt.exception.CustomException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ValidationException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipException;

@ApplicationScoped
public class FileReaderUtil {

//    private static final int THREAD_THRESHOLD = 1000;
//    private static final int BATCH_SIZE = 2500;

    public InvoiceFileResponseDTO readAndConvert(String filePath, String correlationId, String vendorCode) {
        List<InvoiceRecordDTO> allRecords = List.of();
        InvoiceFileResponseDTO response = new InvoiceFileResponseDTO();
        try {
                if (filePath.toLowerCase().endsWith(".csv")) {
                    CsvProcessor csvProcessor = new CsvProcessor(vendorCode);
                    allRecords = csvProcessor.parseCsv(filePath);
                } else if (filePath.toLowerCase().endsWith(".xlsx")) {
                    allRecords = readXlsx(filePath, vendorCode);
                } else{
                throw new CustomException("Only .xlsx and .Csv Files are Supported");
                }
                RowProcessorUtil.validateRecords(allRecords, vendorCode);
                response.setVimInvoiceId(10);
                response.setCorrelationId(correlationId);
                response.setInvoiceList(allRecords);

                JsonWriter.save(response,
                        "C:\\Users\\hitesh.paliwal\\Desktop\\SG Project Files\\Excel File\\Output\\Allow Wheel",
                        "invoice_data");
        } catch (ValidationException ve) {
        throw new CustomException("Record validation failed: " + ve.getMessage());
        } catch (IOException e) {
        throw new CustomException("Failed to save output file. Please check the file path and try again. Error: " + e.getMessage());
        } catch (CustomException e) {
        throw new CustomException("File Not Found at Given Location or Incorrect File name");
        } catch (Exception e){
        throw new CustomException("An unexpected error occurred while processing the file: " + e.getMessage());
        }
        return response;
    }

    private static List<InvoiceRecordDTO> readXlsx(String filePath, String vendorCode) {
        List<InvoiceRecordDTO> allRecords = new ArrayList<>();

        try (OPCPackage pkg = OPCPackage.open(new File(filePath), PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();

            DataFormatter formatter = new DataFormatter();
            SheetProcessor handler = new SheetProcessor(allRecords,vendorCode);
            while (sheets.hasNext()) {
                try (InputStream sheetStream = sheets.next()) {
                    XMLReader parser = XMLReaderFactory.createXMLReader();
                    parser.setContentHandler(new XSSFSheetXMLHandler(
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

//-----------------------------
//sheet processor file
//-----------------------------
    public static class SheetProcessor implements XSSFSheetXMLHandler.SheetContentsHandler {

        private final List<InvoiceRecordDTO> records;
        private final String vendorCode;
        private final Map<String, String> excelToInternalMap;
        private final List<String> expectedHeaders;  // expected header sequence
        private Map<Integer, String> columnIndexToHeaderMap = new HashMap<>();
        private Map<String, Object> currentRowMap;
        private boolean headerRowDetected = false;

        public SheetProcessor(List<InvoiceRecordDTO> records, String vendorCode) {
            this.records = records;
            this.vendorCode = vendorCode;
            this.excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
            this.expectedHeaders = new ArrayList<>(excelToInternalMap.keySet());
        }

        @Override
        public void startRow(int rowNum) {
            currentRowMap = new HashMap<>();
        }

        @Override
        public void endRow(int rowNum) {
            if (!headerRowDetected) {
                if (RowProcessorUtil.isHeaderRow(currentRowMap, excelToInternalMap)) {
//                    System.out.println("currentRowMap" + currentRowMap);
                    for (Map.Entry<String, Object> entry : currentRowMap.entrySet()) {
                        int colIndex = Integer.parseInt(entry.getKey());
                        String rawHeader = entry.getValue().toString().trim().toLowerCase();
                        columnIndexToHeaderMap.put(colIndex, rawHeader);
                    }
                    headerRowDetected = true;
//                System.out.println("Header Row Detected with Mapping: " + columnIndexToHeaderMap);
                }
            } else {
                if (!ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
                    InvoiceRecordDTO dto = RowProcessorUtil.mapToInvoiceDTO(expectedHeaders, columnIndexToHeaderMap, currentRowMap, excelToInternalMap,null);
                    if (RowProcessorUtil.isInvoiceAndClaimEmpty(dto)) {
                        System.out.println("Skipping row " + rowNum + " because Invoice # and Claim # are empty.");
                        return;
                    }
                    records.add(dto);
                }
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int colIndex = ExcelHeaderUtils.getColumnIndex(cellReference);
            currentRowMap.put(String.valueOf(colIndex), formattedValue != null ? formattedValue.trim() : "");
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // No-op
        }
    }
}

//----------------------------------
//CsvProcessor File
//------------------------------------
class CsvProcessor {

    private final List<InvoiceRecordDTO> records = new ArrayList<>();
    private final String vendorCode;
    private final Map<String, String> excelToInternalMap;
    private final List<String> expectedHeaders;
    private final Map<Integer, String> columnIndexToHeaderMap = new HashMap<>();
    private Map<String, Object> currentRowMap;
    private boolean headerRowDetected = false;

    public CsvProcessor(String vendorCode) {
        this.vendorCode = vendorCode;
        this.excelToInternalMap = ExcelHeaderUtils.loadHeaderMapping(vendorCode);
        this.expectedHeaders = new ArrayList<>(excelToInternalMap.keySet());
    }

    public List<InvoiceRecordDTO> parseCsv(String filePath) throws IOException {
        List<String> missingHeaders = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int rowNum = 0;

            while ((line = br.readLine()) != null) {
                rowNum++;
                currentRowMap = new HashMap<>();
                String[] cells = line.split(",", -1);

                for (int i = 0; i < cells.length; i++) {
                    currentRowMap.put(String.valueOf(i), cells[i].trim());
                }

                if (!headerRowDetected) {
                    if (RowProcessorUtil.isHeaderRow(currentRowMap, excelToInternalMap)) {
                        System.out.println("currentRowMap: " + currentRowMap);
                        for (Map.Entry<String, Object> entry : currentRowMap.entrySet()) {
                            int colIndex = Integer.parseInt(entry.getKey());
                            String rawHeader = entry.getValue().toString().trim().toLowerCase();
                            if (rawHeader.isEmpty() && colIndex < expectedHeaders.size()) {
                                rawHeader = expectedHeaders.get(colIndex).toLowerCase(); // ← fallback
                                missingHeaders.add(rawHeader);
                            }

                            columnIndexToHeaderMap.put(colIndex, rawHeader);
                        }
                        headerRowDetected = true;
                        InvoiceRecordDTO dto = new InvoiceRecordDTO();
                        if (!missingHeaders.isEmpty()) {
                            System.out.println("Missing expected headers (defaulted): " + String.join(", ", missingHeaders));
                            dto.setStatus("VALIDATION_FAILED");
                            dto.setStatusDescription("Missing expected headers: " + String.join(", ", missingHeaders));
                        }
//                        System.out.println("Header Row Detected with Mapping: " + columnIndexToHeaderMap);
                    }
                } else {
                    if (!ExcelHeaderUtils.isEmptyRow(currentRowMap)) {
                        InvoiceRecordDTO dto = RowProcessorUtil.mapToInvoiceDTO(
                                expectedHeaders,
                                columnIndexToHeaderMap,
                                currentRowMap,
                                excelToInternalMap,
                                missingHeaders
                        );

                        if (RowProcessorUtil.isInvoiceAndClaimEmpty(dto)) {
                            System.out.println("Skipping row " + rowNum + " because Invoice # and Claim # are empty.");
                            continue;
                        }

                        records.add(dto);
                    }
                }
            }
        }catch (Exception e){
            throw new CustomException("Error While Reading the .csv File");
        }

        return records;
    }
}


//-------------------------
//ExcelHeaderUtils
//---------------------------
class ExcelHeaderUtils {

    public static Map<String, String> loadHeaderMapping(String vendorCode) {
        InputStream is = ExcelHeaderUtils.class.getClassLoader().getResourceAsStream("configs/" + vendorCode + ".json");

        if (is == null) {
            throw new CustomException("Header mapping file not found for vendor: " + vendorCode);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            VendorHeaderMapping mapping = mapper.readValue(is, VendorHeaderMapping.class);

            Map<String, String> headerMap = new LinkedHashMap<>();
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

            Map<String, Boolean> validationMap = new LinkedHashMap<>();
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

//------------------------
//Json File Writer
//-------------------------
class JsonWriter {

    // Use a static ObjectMapper configured once
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        // Register JavaTimeModule to handle Java 8 date/time types like LocalDateTime
        mapper.registerModule(new JavaTimeModule());

        // Optional: serialize dates as ISO strings, not timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static String save(Object obj, String outputDir, String filePrefix) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = filePrefix + "_" + timestamp + ".json";
        File file = new File(outputDir, fileName);

        // Create directory if it doesn't exist
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        mapper.writerWithDefaultPrettyPrinter().writeValue(file, obj);
        return file.getAbsolutePath();
    }
}


//-------------------------
//RowProcessorUtil File
//--------------------------
class RowProcessorUtil {
//    public static boolean isValid = true;
//    public static List<String> errors = new ArrayList<>();

    public static boolean isHeaderRow(Map<String, Object> row, Map<String, String> excelToInternalMap) {
        long matchCount = row.values().stream()
                .filter(val -> val instanceof String)
                .map(val -> ((String) val).toLowerCase())
                .filter(excelToInternalMap::containsKey)
                .count();
        return matchCount >= 3;
    }

    public static Object cleanValue(Object value) {
        if (value == null) return null;

        String val = value.toString().trim();
        val = val.strip();

        List<String> datePatterns = Arrays.asList(
                "M/d/yy", "MM/dd/yy", "M/d/yyyy", "MM/dd/yyyy",
                "yyyy-MM-dd", "yyyy/MM/dd", "dd-MM-yyyy", "dd/MM/yyyy"
        );

        for (String pattern : datePatterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern);
                inputFormat.setLenient(false);
                Date parsedDate = inputFormat.parse(val);
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                return outputFormat.format(parsedDate);
            } catch (Exception ignored) {
            }
        }

        val = val.replaceAll("[^a-zA-Z0-9.\\-/ \\-]", "").strip();
        return val.isBlank() ? null : val;
    }

    public static InvoiceRecordDTO mapToInvoiceDTO(List<String> expectedHeaders,
                                                   Map<Integer, String> columnIndexToHeaderMap,
                                                   Map<String, Object> currentRowMap,
                                                   Map<String, String> excelToInternalMap,
                                                   List<String> missingHeadersOrNull) {

        Map<String, Object> request = new LinkedHashMap<>();
        List<String> missingHeaders = new ArrayList<>();
        missingHeaders.addAll(CollectionUtils.isNotEmpty(missingHeadersOrNull) ? missingHeadersOrNull : new ArrayList<>());
        Map<String, Integer> headerToColumnIndex = new HashMap<>();
        for (Map.Entry<Integer, String> entry : columnIndexToHeaderMap.entrySet()) {
            if (entry.getValue() != null) {
                headerToColumnIndex.put(entry.getValue().trim().toLowerCase(), entry.getKey());
            }
        }

        Set<Integer> availableFallbackIndices = new LinkedHashSet<>();
        for (int i = 0; i < currentRowMap.size(); i++) {
            if (!columnIndexToHeaderMap.containsKey(i)) {
                availableFallbackIndices.add(i);
            }
        }

        for (String expectedHeader : expectedHeaders) {
            String expectedLower = expectedHeader.toLowerCase();
            String internalKey = excelToInternalMap.getOrDefault(expectedLower, expectedLower);
            Object value = null;

            if (headerToColumnIndex.containsKey(expectedLower)) {
                Integer index = headerToColumnIndex.get(expectedLower);
                value = currentRowMap.get(String.valueOf(index));
            } else {
                missingHeaders.add(expectedHeader);
                Iterator<Integer> it = availableFallbackIndices.iterator();
                while (it.hasNext()) {
                    Integer idx = it.next();
                    Object possibleValue = currentRowMap.get(String.valueOf(idx));
                    if (possibleValue != null && !String.valueOf(possibleValue).isBlank()) {
                        value = possibleValue;
                        it.remove();
                        break;
                    }
                }
            }

            request.put(internalKey, cleanValue(value));
        }

        InvoiceRecordDTO dto = new InvoiceRecordDTO();
        dto.setClaimNumber(String.valueOf(request.getOrDefault("claimNumber", "")));
        dto.setInvoiceNumber(String.valueOf(request.getOrDefault("invoiceNumber", "")));
        dto.setRecJson(request);

        if (!missingHeaders.isEmpty()) {
            dto.setStatus("VALIDATION_FAILED");
            dto.setStatusDescription("Missing expected headers: " + String.join(", ", missingHeaders));
        }

        return dto;
    }

    public static void validateRecords(List<InvoiceRecordDTO> records, String vendorCode) {
        LocalDateTime now = LocalDateTime.now();
        String createdBy = "John Doe";

        List<Column> validationRules = ExcelHeaderUtils.loadFullValidationRules(vendorCode);

        for (InvoiceRecordDTO record : records) {
            record.setCreatedDate(now);
            record.setCreatedBy(createdBy);

            List<String> errors = new ArrayList<>();
            boolean isValid = true;

            Map<String, Object> request = record.getRecJson();

            for (Column col : validationRules) {
                String key = col.getInternalName();
                Object value = request.get(key);

                if (col.isRequired() && (value == null || value.toString().isBlank())) {
                    String errorMsg = key + " is required";
                    if (!errors.contains(errorMsg)) {
                        errors.add(errorMsg);
                    }
                    request.put(key, null);
                    isValid = false;
                    continue;
                }

                if (value == null || value.toString().isBlank()) continue;

                String type = col.getType();
                Map<String, Object> constraints = col.getConstraints();
                String errorMsg = "";

                if ("numeric".equalsIgnoreCase(type)) {
                    try {
                        double num = Double.parseDouble(value.toString());
                        if (constraints != null) {
                            if (constraints.containsKey("min") && num < Double.parseDouble(constraints.get("min").toString()))
                                throw new IllegalArgumentException("must be >= " + constraints.get("min"));
                            if (constraints.containsKey("max") && num > Double.parseDouble(constraints.get("max").toString()))
                                throw new IllegalArgumentException("must be <= " + constraints.get("max"));
                        }
                    } catch (Exception e) {
                        errorMsg = e.getMessage() != null ? e.getMessage() : "invalid numeric format";
                        isValid = false;
                    }
                } else if ("string".equalsIgnoreCase(type)) {
                    String str = value.toString();
                    if (constraints != null) {
                        if (constraints.containsKey("minLength") && str.length() < (int) constraints.get("minLength"))
                            errorMsg = "length < " + constraints.get("minLength");
                        else if (constraints.containsKey("maxLength") && str.length() > (int) constraints.get("maxLength"))
                            errorMsg = "length > " + constraints.get("maxLength");
                    }
                    if (!errorMsg.isEmpty()) isValid = false;
                }

                if (!errorMsg.isEmpty()) {
                    if (!errors.contains(key + ": " + errorMsg)) {
                        errors.add(key + ": " + errorMsg);
                    }
                    request.put(key, null);
                }
            }

            if (errors.isEmpty() && !"VALIDATION_FAILED".equals(record.getStatus())) {
                record.setStatus("VALID");
                record.setStatusDescription("Validated successfully");
            } else {
                record.setStatus("VALIDATION_FAILED");

                // Build a set of existing error messages (if any) to avoid duplicates
                Set<String> uniqueMessages = new LinkedHashSet<>();
                if (record.getStatusDescription() != null && !record.getStatusDescription().isBlank()) {
                    String[] existingMessages = record.getStatusDescription().split(";\\s*");
                    for (String msg : existingMessages) {
                        uniqueMessages.add(msg);
                    }
                }
                uniqueMessages.addAll(errors);

                record.setStatusDescription(String.join("; ", uniqueMessages));
            }
        }
    }


    public static boolean isInvoiceAndClaimEmpty(InvoiceRecordDTO dto) {
        String invoice = dto.getInvoiceNumber();
        String claim = dto.getClaimNumber();
        return isNullOrEmptyOrLiteralNull(invoice) && isNullOrEmptyOrLiteralNull(claim);
    }

    static boolean isNullOrEmptyOrLiteralNull(String val) {
        return val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null");
    }

    public static Map<Integer, String> guessHeadersFromPosition(Map<String, Object> rowData, List<String> expectedHeaders) {
        Map<Integer, String> guessedMap = new HashMap<>();
        int index = 0;
        for (String header : expectedHeaders) {
            guessedMap.put(index, header);  // Just map by order
            index++;
        }
        return guessedMap;
    }

}
