package com.example.reader;

import com.example.exception.IncorrectSAPDataException;
import com.example.exception.IncorrectWandDataException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReadWANDexcel {


    public Map<String, List<String>> getMcKinseyTimesheetData(File file) {

        Map<String, List<String>> columnWiseData = new HashMap<>();
        List<String> dataList = new ArrayList<>();
        Workbook myWorkBook = null;
        int empIdRowIndex = 0;
        Short maxColIx;
        String cellData = null;
        Row row = null;
        Cell cell;
        try {
            //creating Workbook instance that refers to .xlsx file

            POIFSFileSystem myFileSystem = new POIFSFileSystem(file);
            myWorkBook = new HSSFWorkbook(myFileSystem);
          /*  if (file.getName().endsWith("xls")) {
                POIFSFileSystem myFileSystem = new POIFSFileSystem(file);
                myWorkBook = new HSSFWorkbook(myFileSystem);
            } else if (file.getName().endsWith("xlsx")) {
                myWorkBook = new XSSFWorkbook(file);
            }*/

            Sheet sheet = myWorkBook.getSheetAt(0);  //creating a Sheet object to retrieve object

            //iterating over excel file
            Iterator<Row> rowIterator = sheet.iterator();
            Pattern pattern = Pattern.compile("[0-9]{2}-[a-zA-Z]{3}");
            outerloop:
            for (Row rowIndex : sheet) {
                maxColIx = rowIndex.getLastCellNum();
                for (short colIx = 0; colIx < maxColIx; colIx++) { //loop from first to last index
                    cell = rowIndex.getCell(colIx); //get the cell
                    if (cell.getStringCellValue().contains("Worker")) {
                        empIdRowIndex = cell.getRowIndex();//2
                        break outerloop;
                    }

                }
                rowIterator.next();
            }
            maxColIx = 0;
            Row headerRow = sheet.getRow(empIdRowIndex);

            for (short colIx = 0; colIx < headerRow.getLastCellNum(); colIx++) {
                cell = headerRow.getCell(maxColIx);
                Matcher m = pattern.matcher(cell.getStringCellValue());
                if (m.matches() || cell.getStringCellValue().contains("Worker"))
                    maxColIx++;
            }

            while (rowIterator.hasNext()) {

                row = rowIterator.next();
                Cell myCell = null;
                cell = row.getCell(0); //get the cell
                if (cell.getStringCellValue().contains("Grand Total")) {
                    break;
                }
                for (int i = 0; i < maxColIx; i++) {
                    // values = new ArrayList<>();
                    cellData = new String();
                    myCell = row.getCell(i);
                    if (myCell != null) {
                        if (myCell.getCellType() == CellType.NUMERIC)
                            cellData = (String.valueOf(myCell.getNumericCellValue()));
                        if (myCell.getCellType() == CellType.STRING) {

                            if (StringUtils.isEmpty(myCell.getRichStringCellValue().toString())) {
                                cellData = "0.0";
                            } else {
                                cellData = myCell.getRichStringCellValue().toString();
                            }
                        }
                        if (myCell.getCellType() == CellType.BLANK)
                            cellData = "0.0";

                        if (empIdRowIndex != myCell.getRowIndex()) {
                            //values.add(cellData);
                            List<String> existingValues = columnWiseData.get(dataList.get(i));
                            if (empIdRowIndex == myCell.getRowIndex()) {
                                columnWiseData.put(dataList.get(i), null);
                            } else if (existingValues != null) {
                                existingValues.add(cellData);
                                columnWiseData.put(dataList.get(i), existingValues);
                            } else if (existingValues == null) {
                                List<String> temp = new ArrayList<>();
                                temp.add(cellData);
                                columnWiseData.put(dataList.get(i), temp);
                            }
                        }
                        dataList.add(cellData);
                    } else {
                        List<String> existingValues = columnWiseData.get(dataList.get(i));
                        if (existingValues != null) {
                            existingValues.add("0.0");
                            columnWiseData.put(dataList.get(i), existingValues);
                        } else if (existingValues == null) {
                            List<String> temp = new ArrayList<>();
                            temp.add("0.0");
                            columnWiseData.put(dataList.get(i), temp);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("WAND dump file is missing");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect WAND data",
                    new IncorrectWandDataException());
        } catch (Exception e) {
        	e.printStackTrace();
        	throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect WAND data",
                    new IncorrectWandDataException());
        }

        Map<String, List<String>> clientDataMap = new HashMap<>();
        List<String> dates = new ArrayList<>();
        for (String key : columnWiseData.keySet()) {
            if (key.contains("-")) {
                dates.add(key);
                clientDataMap.put(key.split("-")[0], columnWiseData.get(key));
            } else {
                clientDataMap.put(key, columnWiseData.get(key));
            }
        }
        clientDataMap.put("Days", dates);
        if(clientDataMap.isEmpty())
        {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect WAND data",
                    new IncorrectWandDataException());
        }
        return clientDataMap;
    }
}
