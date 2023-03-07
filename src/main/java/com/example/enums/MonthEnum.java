package com.example.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MonthEnum
{
    Jan("01"),Feb("02"),Mar("03"),Apr("04"),May("05"),Jun("06"),Jul("07"),Aug("08"),Sep("09"),Oct("10"),Nov("11"),Dec("12");
    private String value;
    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
    MonthEnum(String value) {
        this.value = value;
    }




}
