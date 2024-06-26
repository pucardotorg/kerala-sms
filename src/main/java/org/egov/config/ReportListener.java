package org.egov.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.model.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Slf4j
public class ReportListener {

    private Report report;

    @Autowired
    private ObjectMapper objectMapper;


    @KafkaListener(topics = "${kafka.topics.sms.bounce}")
    public void listen(final HashMap<String, Object> record) {
        Report report = objectMapper.convertValue(record, Report.class);
        log.info(report.toString());
    }

}
