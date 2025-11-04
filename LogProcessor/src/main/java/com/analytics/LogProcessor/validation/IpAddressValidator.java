package com.analytics.LogProcessor.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class IpAddressValidator implements ConstraintValidator<ValidIpAddress,String> {

    private boolean allowNull;

    @Override
    public void initialize(ValidIpAddress constraintAnnotation) {
        this.allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String ip, ConstraintValidatorContext constraintValidatorContext) {
        if(ip == null || ip.trim().isEmpty()) return false;
        try{
            InetAddress.getByName(ip);
            return true;
        }catch (UnknownHostException e){
            log.error("Record with ip: {} not processed since ip is invalid", ip);
            return false;
        }
    }
}
