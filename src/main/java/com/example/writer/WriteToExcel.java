package com.example.writer;

import com.example.enums.MonthEnum;
import com.example.exception.BillingCannotBeGeneratedException;
import com.example.exception.OutputUploadException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTable;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTTableStyleInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WriteToExcel {
    @Autowired
    private UploadOutputFileService uploadOutputFileService;

    /**
     * @param nagarroMap
     * @param proWANDMap
     * @param inverse
     * @author Gunjan
     */
    public String writeEmployeeData(Map<String, List<String>> nagarroMap, Map<String, List<String>> proWANDMap, Map<String, String> inverse, String projectName, String projectCode) throws IOException {
        {
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet("Employee Data");
            List<String> configErrorEmails = new ArrayList<>();
            InputStream azureInputStream = null;
            FileOutputStream out = null;
            int rownum;
            writeHeaderData(sheet);

            //filter out config on basis of input project name
            inverse = inverse.entrySet().stream().filter(map -> map.getValue().contains(projectName))
                    .collect(Collectors.toMap(p -> p.getKey(), p -> p.getValue()));

            Map<String, List<String>> nagarroMapCopy = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : nagarroMap.entrySet()) {
                nagarroMapCopy.put(entry.getKey(), entry.getValue());
            }
            Map<String, List<String>> proWANDMapCopy = new HashMap<>();

            for (Map.Entry<String, List<String>> entry : proWANDMap.entrySet()) {
                proWANDMapCopy.put(entry.getKey(), entry.getValue());
            }

            List<String> excludeEmail  = writeEmployeeNames(nagarroMap, inverse, sheet, configErrorEmails,proWANDMapCopy);
            //nagarroMapCopy.get("Employee Email Address | Date").removeAll(excludeEmail);
            Set<String> nagarroHeaders = nagarroMap.keySet();
            List.of("Employee Email Address | Date", "Full Name", "Employee ID").forEach(nagarroHeaders::remove);

            Set<String> proWANDHeaders = proWANDMap.keySet();
            List.of("Worker", "Days").forEach(proWANDHeaders::remove);

            calculateSAPtotal(inverse, nagarroMapCopy, proWANDMapCopy, sheet, nagarroHeaders);

            calculateWANDtotal(inverse, nagarroMapCopy, proWANDMapCopy, sheet, nagarroHeaders);

            rownum = getDifferenceInTimesheets(inverse, nagarroMapCopy, proWANDMapCopy, sheet, nagarroHeaders, proWANDHeaders);

            rownum = getAdditionalDataRows(nagarroMap, proWANDMap, inverse, nagarroMapCopy, proWANDMapCopy, sheet, rownum);


            try {
                transformExcelInTableObject(workbook, sheet, getExceptionRownum(sheet, configErrorEmails, rownum));
                if(!projectCode.equalsIgnoreCase("00"))
                createBillingWorkSheet(workbook, proWANDMapCopy, projectName, sheet, projectCode,rownum);
                File file = new File("test.xlsx");
                out = new FileOutputStream(file);
                workbook.write(out);
                azureInputStream = new FileInputStream(file);
                return uploadOutputFileService.uploadFileToAzure("mcKinsey", file.getName(), azureInputStream, file.length());
            }

            catch (MaxUploadSizeExceededException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Output File size exceeds than permissible limits.",
                        new OutputUploadException());
            }
            catch (UncheckedIOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please check blob configuration.",
                    new OutputUploadException());
           }
            catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Exception found in generating billing worksheet",
                        new BillingCannotBeGeneratedException());
            }finally {
                out.close();
                azureInputStream.close();
            }
        }
    }


    private List<String> writeEmployeeNames(Map<String, List<String>> nagarroMap, Map<String, String> inverse, XSSFSheet sheet, List<String> configErrorEmails, Map<String, List<String>> proWANDMapCopy) {
        Row row;
        int rownum = 1;
        int nameIterator = 0;
        List<String> excludeEmail = new ArrayList<>();
        for (String empEmail : nagarroMap.get("Employee Email Address | Date")) {
            if (inverse.get(empEmail) == null) {
                configErrorEmails.add(empEmail+"#"+(nagarroMap.get("Full Name").get(nameIterator)));

            }
            else if(!proWANDMapCopy.get("Worker").contains(inverse.get(empEmail).split(":")[0])) {
                System.out.println(empEmail + " will be dealt in no data found in WAND !!");
                excludeEmail.add(empEmail);
            }
            else {

                row = sheet.createRow(rownum++);
                Cell cell = row.createCell(0);
                cell.setCellValue(empEmail);
                cell = row.createCell(1);
                cell.setCellValue(nagarroMap.get("Full Name").get(nameIterator));

            }
            nameIterator++;
        }
        return excludeEmail;
    }

    private int getDifferenceInTimesheets(Map<String, String> inverse, Map<String, List<String>> nagarroMapCopy, Map<String, List<String>> proWANDMapCopy, XSSFSheet sheet, Set<String> nagarroHeaders, Set<String> proWANDHeaders) {
        Row row;
        int rownum = 1;
        int empCount = 0;
        for (String emailID : nagarroMapCopy.get("Employee Email Address | Date")) {
            try {
                if (inverse.get(emailID) == null) {
                    empCount++;
                }
                else if(!proWANDMapCopy.get("Worker").contains(inverse.get(emailID).split(":")[0])) {
                    System.out.println(emailID + " will be dealt in no data found in WAND !!");
                    empCount++;
                }
                else {
                    double sapTotal = 0;
                    double wandTotal = 0;
                    for (String dates : nagarroHeaders) {
                        sapTotal = sapTotal + Double.parseDouble(nagarroMapCopy.get(dates).get(empCount));

                    }
                    for (String dates : proWANDHeaders) {
                        int loc = 0;
                        for (String wandNames : proWANDMapCopy.get("Worker")) {

                            if (wandNames != null && wandNames.contains(inverse.get(emailID).split(":")[0])) {
                                wandTotal = wandTotal + Double.parseDouble(proWANDMapCopy.get(dates).get(loc));
                            } else {
                                loc++;
                            }
                        }

                    }


                    empCount++;
                    if(proWANDMapCopy.get("Worker").contains(inverse.get(emailID).split(":")[0])) {
                        row = sheet.getRow(rownum++);
                        Cell cell = row.createCell(4);

                        Cell cellWand = row.createCell(5);

                        Cell diffCell = row.createCell(6);
                        cell.setCellValue(sapTotal);
                        cellWand.setCellValue(wandTotal);
                        diffCell.setCellValue(Math.abs(wandTotal - sapTotal));
                        cell = row.createCell(7);
                        cell.setCellValue(inverse.get(emailID).split(":")[1]);
                        if (Math.abs(wandTotal - sapTotal) == 0) {

                            cell = row.createCell(8);
                            cell.setCellValue("OK");
                        } else {
                            cell = row.createCell(8);
                            cell.setCellValue("NOK");
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return rownum;
    }

    private void transformExcelInTableObject(XSSFWorkbook workbook, XSSFSheet sheet, int exceptionRownum) {
        AreaReference reference = workbook.getCreationHelper().createAreaReference(
                new CellReference(0, 0), new CellReference(exceptionRownum - 1, 8));

        XSSFTable table = sheet.createTable(reference);
        CTTable cttable = table.getCTTable();

        cttable.setDisplayName("Table1");
        cttable.setId(1);
        cttable.setName("Test");
        for (int i = 0; i < 9; i++)
            table.getCTTable().getTableColumns().getTableColumnArray(i).setId(i + 1);
        table.getCTTable().addNewAutoFilter().setRef(table.getArea().formatAsString());
        CTTableStyleInfo styleInfo = cttable.addNewTableStyleInfo();
        styleInfo.setName("TableStyleMedium2");
        styleInfo.setShowColumnStripes(true);
        styleInfo.setShowRowStripes(true);
    }

    private void writeHeaderData(XSSFSheet sheet) {
        List<String> headersList = new ArrayList<>();
        headersList.add("Email ID");
        headersList.add("Employee Name");
        headersList.add("Additional Days in SAP");
        headersList.add("Additional days in WAND");
        headersList.add("Total hrs in SAP");
        headersList.add("Total hrs in WAND");
        headersList.add("Difference in hrs");
        headersList.add("Project Name");
        headersList.add("Status");

        int columnCount = 0;
        Row row = sheet.createRow(0);
        for (String header : headersList) {
            Cell cell = row.createCell(columnCount++);
            cell.setCellValue(header);
        }

    }

    private void calculateWANDtotal(Map<String, String> inverse, Map<String, List<String>> nagarroMapCopy, Map<String, List<String>> proWANDMapCopy, XSSFSheet sheet, Set<String> nagarroHeaders) {
        Row row;
        int wandOuter = 0;
        int rownum = 1;
        for (String empEmail : nagarroMapCopy.get("Employee Email Address | Date")) {
            if (inverse.get(empEmail) == null) {
                System.out.println(empEmail + " does not exist in mapping file !!");
            }
            else if(!proWANDMapCopy.get("Worker").contains(inverse.get(empEmail).split(":")[0])) {
                System.out.println(empEmail + " will be dealt in no data found in WAND !!");

            } else {
                row = sheet.getRow(rownum++);
                Cell cell = row.createCell(3);
                List<String> addSAP = new ArrayList<>();
                for (String dates : nagarroHeaders) {
                    int loc = 0;
                    for (String wandNames : proWANDMapCopy.get("Worker")) {
                        if (wandNames != null && wandNames.contains(inverse.get(empEmail).split(":")[0]) && proWANDMapCopy.get(dates)!=null && proWANDMapCopy.get(dates).get(loc)!=null && nagarroMapCopy.get(dates)!=null && nagarroMapCopy.get(dates).get(wandOuter)!=null && Double.parseDouble(proWANDMapCopy.get(dates).get(loc)) > 0 && Double.parseDouble(proWANDMapCopy.get(dates).get(loc)) > Double.parseDouble(nagarroMapCopy.get(dates).get(wandOuter)))
                            addSAP.add(dates);
                        else
                            loc++;

                    }
                }

                cell.setCellValue(addSAP.toString());
            }
            wandOuter++;
        }
    }

    private void calculateSAPtotal(Map<String, String> inverse, Map<String, List<String>> nagarroMapCopy, Map<String, List<String>> proWANDMapCopy, XSSFSheet sheet, Set<String> nagarroHeaders) {
        Row row;
        int outer = 0;
        int rownum = 1;
        for (String empEmail : nagarroMapCopy.get("Employee Email Address | Date")) {
            if (inverse.get(empEmail) == null) {
                System.out.println(empEmail + " does not exist in mapping file !!");
            }
            else if(!proWANDMapCopy.get("Worker").contains(inverse.get(empEmail).split(":")[0])) {
                System.out.println(empEmail + " will be dealt in no data found in WAND !!");

            }
            else {
                row = sheet.getRow(rownum++);
                Cell cell = row.createCell(2);
                List<String> addSAP = new ArrayList<>();

                for (String dates : nagarroHeaders) {
                    int loc = 0;
                    for (String wandNames : proWANDMapCopy.get("Worker")) {

                        if (wandNames != null && wandNames.contains(inverse.get(empEmail).split(":")[0])) {
                            if (nagarroMapCopy.get(dates) == null || (nagarroMapCopy.get(dates) != null && nagarroMapCopy.get(dates).get(outer) == null))
                                System.out.println(dates + "  " + outer + "SAP " + empEmail);
                            if (proWANDMapCopy.get(dates) == null || (proWANDMapCopy.get(dates) != null && proWANDMapCopy.get(dates).get(loc) == null))
                                System.out.println(dates + "  " + loc + "WAND " + empEmail);
                            else if (Double.parseDouble(nagarroMapCopy.get(dates).get(outer)) > 0 && Double.parseDouble(proWANDMapCopy.get(dates).get(loc)) < Double.parseDouble(nagarroMapCopy.get(dates).get(outer)))
                                addSAP.add(dates);
                        } else {
                            loc++;
                        }
                    }
                }
                cell.setCellValue(addSAP.toString());


            }
            outer++;
        }
    }

    private int getAdditionalDataRows(Map<String, List<String>> nagarroMap, Map<String, List<String>> proWANDMap, Map<String, String> inverse, Map<String, List<String>> nagarroMapCopy, Map<String, List<String>> proWANDMapCopy, XSSFSheet sheet, int exceptionRownum) {
        Row row;
        Cell cell;
        for (String email : inverse.keySet()) {
            try {
                if (!nagarroMapCopy.get("Employee Email Address | Date").contains(email)) {

                    int loc = 0;
                    for (String str : proWANDMapCopy.get("Worker")) {
                        if (str.equalsIgnoreCase(inverse.get(email).split(":")[0])) {
                            break;
                        } else
                            loc++;
                    }
                    if (loc < proWANDMapCopy.get("Worker").size()) {

                        List<String> days = new ArrayList<>();
                        for (String dates : proWANDMap.keySet()) {
                            if (Double.parseDouble(proWANDMap.get(dates).get(loc)) > 0) {
                                days.add(dates);
                            }

                        }
                        row = sheet.createRow(exceptionRownum++);
                        cell = row.createCell(0);
                        cell.setCellValue(email);
                        cell = row.createCell(1);
                        cell.setCellValue(inverse.get(email).split(":")[0]);
                        cell = row.createCell(2);
                        cell.setCellValue("No record in SAP");
                        cell = row.createCell(3);
                        cell.setCellValue(days.toString());
                        cell = row.createCell(4);
                        cell.setCellValue(String.valueOf(0));
                        cell = row.createCell(5);
                        cell.setCellValue(days.size() * 8);
                        cell = row.createCell(6);
                        cell.setCellValue(days.size() * 8);
                        cell = row.createCell(7);
                        cell.setCellValue(inverse.get(email).split(":")[1]);
                        cell = row.createCell(8);
                        cell.setCellValue("NOK");
                    }
                }
                if (!proWANDMapCopy.get("Worker").contains(inverse.get(email).split(":")[0])) {

                    int loc = 0;
                    for (String empIdsInSAP : nagarroMapCopy.get("Employee Email Address | Date")) {
                        if (empIdsInSAP.equalsIgnoreCase(email)) {
                            break;
                        } else
                            loc++;
                    }
                    if (loc < nagarroMapCopy.get("Employee Email Address | Date").size()) {

                        List<String> days = new ArrayList<>();
                        for (String dates : nagarroMap.keySet()) {
                            if (Double.parseDouble(nagarroMap.get(dates).get(loc)) > 0) {
                                days.add(dates);//sort in asc
                            }

                        }
                        row = sheet.createRow(exceptionRownum++);
                        cell = row.createCell(0);
                        cell.setCellValue(email);
                        cell = row.createCell(1);
                        cell.setCellValue(inverse.get(email).split(":")[0]);
                        cell = row.createCell(2);
                        cell.setCellValue(days.toString());
                        cell = row.createCell(3);
                        cell.setCellValue("No record in WAND");
                        cell = row.createCell(4);
                        cell.setCellValue(days.size() * 8);
                        cell = row.createCell(5);
                        cell.setCellValue(String.valueOf(0));
                        cell = row.createCell(6);
                        cell.setCellValue(days.size() * 8);
                        cell = row.createCell(7);
                        cell.setCellValue(inverse.get(email).split(":")[1]);
                        cell = row.createCell(8);
                        cell.setCellValue("NOK");

                    }
                }
            } catch (Exception e) {
                System.out.println("No record in SAP/WAND " + e.getMessage());
            }
        }
        return exceptionRownum;
    }

    private int getExceptionRownum(XSSFSheet sheet, List<String> configErrorEmails, int exceptionRownum) {
        Row row;
        Cell cell;
        for (String email : configErrorEmails) {
            row = sheet.createRow(exceptionRownum++);
            cell = row.createCell(0);
            cell.setCellValue(email.substring(0,email.indexOf("#")));
            cell = row.createCell(1);
            cell.setCellValue(email.substring(email.indexOf("#")+1));
            cell = row.createCell(2);
            cell.setCellValue("");
            cell = row.createCell(3);
            cell.setCellValue("");
            cell = row.createCell(4);
            cell.setCellValue("");
            cell = row.createCell(5);
            cell.setCellValue("");
            cell = row.createCell(6);
            cell.setCellValue("");
            cell = row.createCell(7);
            cell.setCellValue("");
            cell = row.createCell(8);
            cell.setCellValue("ERROR");
        }
        return exceptionRownum;
    }

    private XSSFSheet createBillingWorkSheet(XSSFWorkbook workbook, Map<String, List<String>> proWANDMapCopy, String projectName, XSSFSheet sheet, String projectCode,int maxrows) {

        XSSFSheet billingSheet = workbook.createSheet("McKinsey Billing");
        proWANDMapCopy.put("Days",proWANDMapCopy.get("Days").stream().sorted().collect(Collectors.toList()));
        List<String> headersList = new ArrayList<>();
        CellStyle cellStyle = billingSheet.getWorkbook().createCellStyle();
        Font font = billingSheet.getWorkbook().createFont();
        font.setBold(true);
        font.setFontName("Calibri");
        font.setFontHeightInPoints((short) 10);
        cellStyle.setRotation((short) 90);
        cellStyle.setFont(font);
        headersList.add("First Name");
        headersList.add("Last Name");
        headersList.add("WAND Name");
        headersList.add("Project");
        //conversion of 01-Aug format to 01.08.2022
        int year = LocalDate.now().getYear();
        String month = proWANDMapCopy.get("Days").get(0).split("\\-")[1];
        String intValue = getIntValueForMonth(month);

        for (String days : proWANDMapCopy.get("Days"))
            headersList.add(days.split("\\-")[0]+"."+intValue+"."+year);

        headersList.add("Total Billable Hours");
        headersList.add("Billing Currency");
        headersList.add("Hourly Rate");
        headersList.add("Total Bill Amount");
        headersList.add("Status");
        headersList.add("Total Hours in SAP");

        Cell cell = null;
        int rownum = 0;
        int columnCount = 0;
        int col ;
        Row row = billingSheet.createRow(rownum++);
        for (String header : headersList) {
            cell = row.createCell(columnCount++);
            cell.setCellValue(header);
            cell.setCellStyle(cellStyle);
        }
        row = billingSheet.createRow(rownum++);
        for (int i = sheet.getFirstRowNum()+1; i < maxrows; i++) {

            int index = getIndexByName(proWANDMapCopy, sheet.getRow(i).getCell(1).toString().split("\\ ")[0], sheet.getRow(i).getCell(1).toString().split("\\ ")[1]);
            if (index< proWANDMapCopy.get("Worker").size() && (sheet.getRow(i).getCell(8).toString().contains("OK") || sheet.getRow(i).getCell(8).toString().contains("NOK"))) {

                cell = row.createCell(0);
                cell.setCellValue(sheet.getRow(i).getCell(1).toString().split("\\ ")[0]);
                cell = row.createCell(1);
                cell.setCellValue(sheet.getRow(i).getCell(1).toString().split("\\ ")[1]);
                cell = row.createCell(2);
                cell.setCellValue(proWANDMapCopy.get("Worker").get(index));
                cell = row.createCell(3);
                cell.setCellValue(projectName);
                col = 4;

                for (String days : proWANDMapCopy.get("Days")) {
                    List<String> hours = proWANDMapCopy.get(days.split("\\-")[0]);
                    cell = row.createCell(col);
                    if(hours.size()<=index)
                    {
                        System.out.println(row.getCell(0).toString() + " " + row.getCell(1).toString());
                        break;
                    }
                    else {
                        cell.setCellValue(hours.get(index));
                    }

                    col++;
                }
                cell = row.createCell(col++);
                cell.setCellValue(sheet.getRow(i).getCell(5).toString());
                cell = row.createCell(col++);
                cell.setCellValue("USD");
                cell = row.createCell(col++);
                cell.setCellValue(projectCode);
                cell = row.createCell(col++);
                cell.setCellValue("$"+Double.parseDouble(sheet.getRow(i).getCell(5).toString()) * Double.parseDouble(projectCode));
                cell = row.createCell(col++);
                cell.setCellValue(sheet.getRow(i).getCell(8).toString());
                cell = row.createCell(col++);
                cell.setCellValue(sheet.getRow(i).getCell(4).toString());
                row = billingSheet.createRow(rownum++);
            }
        }
        return billingSheet;
    }

    private String getIntValueForMonth(String month) {
        String intValue=null;
        switch (month)
        {
            case "Jan":
                intValue = MonthEnum.Jan.toString();
                break;
            case "Feb":
                intValue = MonthEnum.Feb.toString();
                break;
            case "Mar":
                intValue = MonthEnum.Mar.toString();
                break;
            case "Apr":
                intValue = MonthEnum.Apr.toString();
                break;
            case "May":
                intValue = MonthEnum.May.toString();
                break;
            case "Jun":
                intValue = MonthEnum.Jun.toString();
                break;
            case "Jul":
                intValue = MonthEnum.Jul.toString();
                break;
            case "Aug":
                intValue = MonthEnum.Aug.toString();
                break;
            case "Sep":
                intValue = MonthEnum.Sep.toString();
                break;
            case "Oct":
                intValue = MonthEnum.Oct.toString();
                break;
            case "Nov":
                intValue = MonthEnum.Nov.toString();
                break;
            case "Dec":
                intValue = MonthEnum.Dec.toString();
                break;
            default:
                System.out.println("INVALID MONTH NAME");

        }
        return intValue;
    }

    private int getIndexByName(Map<String, List<String>> proWANDMapCopy, String firstName, String lastName) {
        int i = 0;
        for (String fullname : proWANDMapCopy.get("Worker")) {
            if (fullname.contains(firstName) && fullname.contains(lastName))
                return i;
            else
                i++;
        }
        return i;
    }
}
