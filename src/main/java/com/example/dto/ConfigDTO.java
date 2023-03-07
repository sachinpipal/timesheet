package com.example.dto;

public class ConfigDTO {
    private String emailAddress;
    private String proWandName;

   /* public ConfigDTO(String emailAddress, String proWandName, String projectName) {
        this.emailAddress = emailAddress;
        this.proWandName = proWandName;
        this.projectName = projectName;
    }*/

    public String getProWandName() {
        return proWandName;
    }

    public void setProWandName(String proWandName) {
        this.proWandName = proWandName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    private String projectName;
}
