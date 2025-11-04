package com.analytics.LogProcessor.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Valid category values accepted by Enrichment Service
 * Based on : https://api.heyering.com/ca6b7066/docs
 */
public enum Category {

    CONTENT_INJECTION("contentinjection"),

    DRIVE_BY_COMPROMISE("drivebycompromise"),

    EXPLOIT_PUBLIC_FACING_APPLICATIONS("exploitpublicfacingapplication"),

    EXTERNAL_REMOTE_SERVICES("externalremoteservices"),

    HARDWARE_ADDITIONS("hardwareadditions"),

    PHISHING("phishing"),

    REPLICATION_THROUGH_REMOVABLE_MEDIA("replicationthroughremovablemedia"),

    SUPPLY_CHAIN_COMPROMISE("supplychaincompromise"),

     TRUSTED_RELATIONSHIP("trustedrelationship"),

     VALID_ACCOUNTS("validaccounts");

     private final String value;

    Category(String value){
        this.value = value;
    }

    /**
     * Get the category value that is sent to Enrichment API
     */
    @JsonValue
    public String getValue(){
        return value;
    }

    @JsonCreator
    public static Category fromString(String value){
        if(value==null) return null;

        String normalised = value.toLowerCase().trim();

        return Arrays.stream(values())
                .filter(category -> category.value.equals(normalised))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid category value: "+value));
    }

    public static boolean isValid(String value){
        if(value == null) return false;
        try{
            fromString(value);
            return true;
        }catch(IllegalArgumentException e){
            return false;
        }
    }

    @Override
    public String toString(){
        return value;
    }
}
