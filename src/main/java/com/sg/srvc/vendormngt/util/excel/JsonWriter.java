package com.sg.srvc.vendormngt.util.excel;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class JsonWriter {
    public static String save(Object obj, String outputDir, String filePrefix) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = filePrefix + "_" + timestamp + ".json";
        File file = new File(outputDir, fileName);
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, obj);
        return file.getAbsolutePath();
    }
}
