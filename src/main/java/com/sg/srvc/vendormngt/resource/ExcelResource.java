package com.sg.srvc.vendormngt.resource;

import com.sg.srvc.vendormngt.dto.ExcelRequestDTO;
import com.sg.srvc.vendormngt.dto.InvoiceFileResponseDTO;
import com.sg.srvc.vendormngt.service.ExcelService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/convertxltojson")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExcelResource {

    @Inject
    ExcelService excelService;

    @POST
//    @Path("/convert")
    public InvoiceFileResponseDTO convertExcel(@Valid ExcelRequestDTO requestDTO) {
        System.out.println("API HITS");
        return excelService.processExcelFile(requestDTO);
    }

    @GET
    public String greet(){
        System.out.println("GREET HITS");
        return "Hello from quarkus";
    }
}
