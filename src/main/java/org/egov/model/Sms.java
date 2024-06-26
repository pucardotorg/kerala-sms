package org.egov.model;

import lombok.*;
import org.egov.model.enums.SmsContentType;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Setter
public class Sms {

    private String mobileNumber;
    private String message;
    private Category category;
    private Long expiryTime;
    private String templateId;

    private SmsContentType contentType;

    public boolean isValid() {

        return isNotEmpty(mobileNumber) && isNotEmpty(message);
    }
}
