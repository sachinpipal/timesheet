package com.example.controller;

import com.example.dto.ConfigDTO;
import com.example.dto.MapUtility;
import com.example.enums.ResponseCodes;
import com.example.exception.InvalidFileFormatException;
import com.example.exception.OutputUploadException;
import com.example.reader.ReadNagarroCSV;
import com.example.reader.ReadWANDexcel;
import com.example.writer.WriteToExcel;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("mcKinsey")
public class AttendanceController {

    @Autowired
    private ReadNagarroCSV readNagarroCSV;
    @Autowired
    private WriteToExcel writeToExcel;
    @Autowired
    private ReadWANDexcel readWANDexcel;

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @ApiOperation(value = "This API is used to get list of timesheet defaulters.")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFiles(@RequestParam("files") List<MultipartFile> files) {

        MapUtility maps = new MapUtility();

            try{
            files.stream().forEach(file -> {

                        try {
                            if (file.getOriginalFilename().endsWith("xlsx") && !file.getOriginalFilename().contains("WAND")) {
                                String str= file.getOriginalFilename();

                                FileInputStream fis = (FileInputStream) file.getInputStream();
                                maps.setNagarroMap(readNagarroCSV.getNagarroData(fis));
                                maps.setProjectName(str.substring(0,str.indexOf(".")));
                                System.out.println(file.getOriginalFilename()+" file processed.");
                            }
                            else  if (file.getOriginalFilename().contains(".csv")) {
                                maps.setProWandEmpMap(loadDataIntoMap(file.getInputStream()));
                                maps.setProjectCode(file.getOriginalFilename().substring(0,file.getOriginalFilename().indexOf(".")));
                                System.out.println(file.getOriginalFilename()+" file processed.");
                            }
                            else if (file.getOriginalFilename().contains("WAND")) {
                                File wandfile = new File("WAND.xlsx");
                                FileOutputStream outputStream = new FileOutputStream(wandfile);
                                IOUtils.copy(file.getInputStream(), outputStream);
                                maps.setProWANDTimesheetData(readWANDexcel.getMcKinseyTimesheetData(wandfile));
                                System.out.println(file.getOriginalFilename()+" file processed.");
                            }
                            else
                            {
                            	System.out.println(file.getOriginalFilename()+" Incorrect file  extension.");
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect file extension(s) found.",
                                        new InvalidFileFormatException());
                            }

                        }
                        catch (ResponseStatusException responseStatusException) {
                        	responseStatusException.printStackTrace();
                            throw new ResponseStatusException(responseStatusException.getStatus(), responseStatusException.getReason());
                        }
                        catch (Exception e) {
                        	e.printStackTrace();
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Something wrong with input streams");
                        }
                    }
            );

                    if(maps.getProjectName()!=null && maps.getProWandEmpMap()!=null && maps.getNagarroMap()!=null && maps.getProWANDTimesheetData()!=null)
                        maps.setBloburl(writeToExcel.writeEmployeeData(maps.getNagarroMap(), maps.getProWANDTimesheetData(), maps.getProWandEmpMap(),maps.getProjectName(),maps.getProjectCode()));

            }  catch (ResponseStatusException resEx) {
                return ResponseEntity.status(resEx.getStatus()).body(resEx.getReason());
            }
            catch (Exception e) {
            	e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Something wrong with input streams");
               // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

        /*    backup code to return the output file as the response type instead of uploading the same to azure BLOB
            try
            {
                String excelPath = Paths.get("test.xlsx")
                        .toAbsolutePath().normalize().toString();
                outputfile = new File(excelPath);
                Path path = Paths.get(outputfile.getAbsolutePath());

            }
            catch (Exception exception) {
                new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
           headers.add("Content-Disposition", "attachment; filename=" + outputfile.getName());
            headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(outputfile));
            MediaType mediaType = MediaType.parseMediaType("application/xml");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + outputfile.getName())
                    .contentType(mediaType)
                    .contentLength(outputfile.length()) //
                    .body(resource);*/




        return new ResponseEntity<String>(maps.getBloburl(), HttpStatus.OK);
    }
    private static boolean validateCSVHeaders(String[] headerValues) {
        String headerLine = "emailAddress|proWandName|projectName";
        String[] columArr = split(headerLine, "\\|");
        for (int i = 0; i < columArr.length; i++) {
            if (!headerValues[i].equalsIgnoreCase(columArr[i].trim())) {
                return true;
            }
        }
        System.out.println("csv column names are validated successfully.");
        return true;
    }
    private static String[] split(String str, String strSeparator) {
        if (str == null || strSeparator == null) {
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(str, strSeparator);
        String[] strArrValues = new String[tokenizer.countTokens()];
        int count = 0;
        while (tokenizer.hasMoreTokens()) {
            strArrValues[count++] = tokenizer.nextToken().trim();
        }
        return strArrValues;
    }
    private static  Map<String, String> loadDataIntoMap(InputStream is)  throws Exception{
        Map<String, String> namesEmpIdMap = new HashMap<>();

            try (Reader fr = new InputStreamReader(is);
                 ICsvBeanReader beanReader = new CsvBeanReader(fr, CsvPreference.STANDARD_PREFERENCE)) {

                String[] headerLine = new String[]{"emailAddress", "proWandName", "projectName"};
                final String[] header = beanReader.getHeader(true);
                if (validateCSVHeaders(header)) {
                    ConfigDTO configDTO;
                    while ((configDTO = beanReader.read(ConfigDTO.class, headerLine)) != null) {
                       String str = configDTO.getProWandName().trim().concat(":").concat(configDTO.getProjectName().trim());
                        namesEmpIdMap.put(configDTO.getEmailAddress(),str);
                    }
                }

            }
            catch (FileNotFoundException e) {
            	e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Something wrong with CSV file");
            }
           return namesEmpIdMap;
    }
}
