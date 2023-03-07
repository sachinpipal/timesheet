package com.example.dto;

import com.google.common.collect.BiMap;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MapUtility {
    public String getBloburl() {
        return bloburl;
    }

    public void setBloburl(String bloburl) {
        this.bloburl = bloburl;
    }

    private Map<String, List<String>> nagarroMap = null;

    public String getProjectName() {
        return projectName;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    private Map<String, String> proWandEmpMap = null;
    private String bloburl = null;
    private Map<String, List<String>> proWANDTimesheetData = null;
    private String projectName = null;
    private String projectCode = null;

    public Map<String, List<String>> getProWANDTimesheetData() {
        return proWANDTimesheetData;
    }

    public void setProWANDTimesheetData(Map<String, List<String>> proWANDTimesheetData) {
        this.proWANDTimesheetData = proWANDTimesheetData;
    }

    public Map<String, List<String>> getNagarroMap() {
        return nagarroMap;
    }

    public void setNagarroMap(Map<String, List<String>> nagarroMap) {
        this.nagarroMap = nagarroMap;
    }

    public Map<String, String> getProWandEmpMap() {
        return proWandEmpMap;
    }

    public void setProWandEmpMap(Map<String, String> proWandEmpMap) {
        this.proWandEmpMap = proWandEmpMap;
    }

}
