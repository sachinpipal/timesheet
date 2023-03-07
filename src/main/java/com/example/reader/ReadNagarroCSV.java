package com.example.reader;

import com.example.exception.IncorrectSAPDataException;
import com.example.exception.InvalidFileFormatException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ReadNagarroCSV {

    public Map<String, List<String>> getNagarroData(FileInputStream fis) {
        Map<String, List<String>> columnWiseData = new HashMap<>();
        Map<String, List<String>> nagarroDataMap = new HashMap<>();
        List<String> dataList = new ArrayList<>();
        String cellData = null;
        Row row;
        Cell myCell;
        int empIdRowIndex = 0;
        short maxColIx;
        try {

            //creating Workbook instance that refers to .xlsx file
            Workbook wb = new XSSFWorkbook(fis);
            Sheet sheet = wb.getSheetAt(0);     //creating a Sheet object to retrieve object

            //iterating over excel file


            outerloop:
            for (Row currentRow : sheet) {
                short minColIx = currentRow.getFirstCellNum(); //get the first column index for a row
                maxColIx = currentRow.getLastCellNum(); //get the last column index for a row
                for (short colIx = minColIx; colIx < maxColIx; colIx++) { //loop from first to last index
                    Cell cell = currentRow.getCell(colIx); //get the cell
                    if (cell.getStringCellValue().contains("Employee Email Address | Date")) {
                        empIdRowIndex = cell.getRowIndex();
                        break outerloop;
                    }
                }

            }

            maxColIx = 0;
            String month = null;
            Pattern pattern = Pattern.compile("[0-9]{2}.[0-9]{2}.[0-9]{4}");
            Row headerRow = sheet.getRow(empIdRowIndex);
            Cell cell = null;
            for (short colIx = 0; colIx < headerRow.getLastCellNum(); colIx++) {
                cell = headerRow.getCell(maxColIx);
                Matcher m = pattern.matcher(cell.getStringCellValue());
                if (m.matches() || cell.getStringCellValue().contains("Employee Email Address | Date") || cell.getStringCellValue().contains("Work Item ID") || cell.getStringCellValue().contains("Employee ID") || cell.getStringCellValue().contains("Full Name")) {
                    maxColIx++;
                    if (m.matches())
                        month = cell.getStringCellValue().substring(cell.getStringCellValue().indexOf(".") + 1, cell.getStringCellValue().indexOf(".") + 3);
                }
            }

            Iterator<Row> rowIterator = sheet.iterator();

            for (int iterator = 0; iterator < empIdRowIndex; iterator++) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {

                row = rowIterator.next();

                for (int i = 1; i < maxColIx; i++) {
                    myCell = row.getCell(i);
                    if(null!=myCell) {
                    if (myCell.getCellType() == CellType.NUMERIC) {
                        cellData = (String.valueOf(myCell.getNumericCellValue()));
                        if (cellData != null && cellData.endsWith(".0"))
                            cellData = cellData.replaceAll("\\.0", "");
                    }

                    if (myCell.getCellType() == CellType.STRING && myCell.getRichStringCellValue() != null)
                        cellData = (myCell.getRichStringCellValue().toString());


                    if (empIdRowIndex != myCell.getRowIndex()) {

                        List<String> existingValues = columnWiseData.get(dataList.get(i - 1));
                        if (empIdRowIndex + 1 == myCell.getRowIndex()) {
                            columnWiseData.put(dataList.get(i - 1), null);
                        }

                        if (existingValues != null) {
                            existingValues.add(cellData);
                            columnWiseData.put(dataList.get(i - 1), existingValues);
                        }
                        if (columnWiseData.get(dataList.get(i - 1)) == null) {
                            List<String> temp = new ArrayList<>();
                            temp.add(cellData);
                            columnWiseData.put(dataList.get(i - 1), temp);
                        }
                    }
                    dataList.add(cellData);
                }
              }
            }


            for (String key : columnWiseData.keySet()) {
                if (key.contains(".")) {

                    nagarroDataMap.put(key.split("\\.")[0], columnWiseData.get(key));//only dates
                } else {
                    nagarroDataMap.put(key, columnWiseData.get(key));
                }
            }
            if(nagarroDataMap.isEmpty())
            {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Incorrect SAP data",
                        new IncorrectSAPDataException());
            }
            return nagarroDataMap;

        } catch (FileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SAP data not uploaded.",
                    new IncorrectSAPDataException());
        } catch (Exception e) {
        	//e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exception occurred while reading SAP data",
                    new IncorrectSAPDataException());
        }
    }

}



