package com.logistics.routing.adapter.out.persistence;

import java.util.Arrays;

public enum AdrClass {
    CLASS_1("1"),
    CLASS_2("2"),
    CLASS_3("3"),
    CLASS_4_1("4.1"),
    CLASS_4_2("4.2"),
    CLASS_4_3("4.3"),
    CLASS_5_1("5.1"),
    CLASS_5_2("5.2"),
    CLASS_6_1("6.1"),
    CLASS_6_2("6.2"),
    CLASS_7("7"),
    CLASS_8("8"),
    CLASS_9("9"),
    CLASS_X("X");

    private final String code;

    AdrClass(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AdrClass fromCode(String code) {
        return Arrays.stream(values())
            .filter(value -> value.code.equals(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown ADR class: " + code));
    }
}