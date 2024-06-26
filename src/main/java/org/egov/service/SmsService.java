package org.egov.service;

import lombok.extern.slf4j.Slf4j;
import org.egov.config.SMSProperties;
import org.egov.model.Category;
import org.egov.model.Sms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class SmsService {

    private CDACSmsClient cdacSmsClient;

    protected SMSProperties smsProperties;

    @Autowired
    public SmsService(CDACSmsClient cdacSmsClient, SMSProperties smsProperties) {
        this.cdacSmsClient = cdacSmsClient;
        this.smsProperties = smsProperties;
    }

    public void sendSMS(Sms sms) {
        if (!sms.isValid()) {
            log.error(String.format("Sms %s is not valid", sms));
            return;
        }

        if (smsProperties.isNumberBlacklisted(sms.getMobileNumber())) {
            log.error(String.format("Sms to %s is blacklisted", sms.getMobileNumber()));
            return;
        }

        if (!smsProperties.isNumberWhitelisted(sms.getMobileNumber())) {
            log.error(String.format("Sms to %s is not in whitelist", sms.getMobileNumber()));
            return;
        }

        submitToExternalSmsService(sms);
    }

    private void submitToExternalSmsService(Sms sms) {
        try {
            // TODO: remove default mobile number for production
            if (smsProperties.isDefaultMobileNumber()) sms.setMobileNumber(smsProperties.getDefaultMobileNumber());

            if (sms.getCategory() == Category.OTP) cdacSmsClient.sendOtpSMS(sms, smsProperties);
            if (sms.getCategory() == Category.NOTIFICATION)
            {
                switch (sms.getContentType())
                {
                    case TEXT:
                        cdacSmsClient.sendSingleSMS(sms, smsProperties);
                        break;
                    case UNICODE:
                        cdacSmsClient.sendUnicodeSMS(sms, smsProperties);
                        break;
                    default:
                        break;
                }
            }
        } catch (RestClientException e) {
            log.error("Error occurred while sending SMS to " + sms.getMobileNumber(), e);
            throw e;
        }
    }
}
