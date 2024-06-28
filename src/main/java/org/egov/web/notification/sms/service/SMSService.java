package org.egov.web.notification.sms.service;

import org.egov.web.notification.sms.models.Sms;
import org.springframework.stereotype.Service;

@Service
public interface SMSService {
    void sendSMS(Sms sms);
}

