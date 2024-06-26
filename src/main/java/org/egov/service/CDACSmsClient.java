package org.egov.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.egov.config.SMSProperties;
import org.egov.model.Sms;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@Slf4j
public class CDACSmsClient {

    private RestTemplate restTemplate;

    @Autowired
    public CDACSmsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Send Single text SMS
     * @return {@link String} response from Mobile Seva Gateway e.g. '402,MsgID = 150620161466003974245msdgsms'
     * @see
     */

    public String sendSingleSMS(Sms sms, SMSProperties smsProperties)
    {
        return sendSMS(sms, smsProperties,false, "singlemsg");
    }

    /**
     * Send Bulk text SMS
     * @return {@link String} response from Mobile Seva Gateway e.g. '402,MsgID = 150620161466003974245msdgsms'
     * @see
     */
    public String sendBulkSMS(Sms sms, SMSProperties smsProperties)
    {
        return sendSMS(sms, smsProperties, true, "bulkmsg");
    }

    /**
     * Send Unicode text SMS
     * @return {@link String} response from Mobile Seva Gateway e.g. '402,MsgID = 150620161466003974245msdgsms'
     * @see
     */
    public String sendUnicodeSMS(Sms sms, SMSProperties smsProperties)
    {
        String message = sms.getMessage();
        String finalmessage = "";
        for (int i = 0; i < message.length(); i++) {

            char ch = message.charAt(i);
            int j = (int) ch;
            String sss = "&#" + j + ";";
            finalmessage = finalmessage + sss;
        }

        return sendSMS(sms, smsProperties, true, "unicodemsg");
    }

    /**
     * Send Single OTP text SMS
     * <p>
     * Use only in case of OTP related message
     * <p>
     * Messages other than OTP will not be delivered to the users
     * @return {@link String} response from Mobile Seva Gateway e.g. '402,MsgID = 150620161466003974245msdgsms'
     * @see
     */
    public String sendOtpSMS(Sms sms, SMSProperties smsProperties)
    {
        return sendSMS(sms, smsProperties,false, "otpmsg");
    }

    /**
     * Send Single Unicode OTP text SMS
     * @return {@link String} response from Mobile Seva Gateway e.g. '402,MsgID = 150620161466003974245msdgsms'
     * @see
     */
    public String sendUnicodeOtpSMS(Sms sms, SMSProperties smsProperties)
    {
        String message = sms.getMessage();
        String finalmessage = "";
        for (int i = 0; i < message.length(); i++) {

            char ch = message.charAt(i);
            int j = (int) ch;
            String sss = "&#" + j + ";";
            finalmessage = finalmessage + sss;
        }

        return sendSMS(sms, smsProperties, false, "unicodeotpmsg");
    }

    public String sendSMS(Sms sms, SMSProperties smsProperties, boolean isBulk, String smsServiceType) {
        String smsProviderURL = smsProperties.getUrl();
        String username = smsProperties.getUsername();
        String password = smsProperties.getPassword();
        String senderId = smsProperties.getSenderid();
        String secureKey = smsProperties.getSecureKey();

        String mobileNumber = sms.getMobileNumber();
        String message = sms.getMessage();
        String templateId = sms.getTemplateId();

        String responseString = "";
        SSLConnectionSocketFactory scf;
        SSLContext context = null;
        String encryptedPassword = "";

        try
        {
            context = SSLContext.getInstance("TLSv1.2"); // Use this line for Java version 7 and above
            context.init(null, null, null);
            scf = new SSLConnectionSocketFactory(context);

            HttpClient httpClient = HttpClients.custom()
                    .setSSLSocketFactory(scf)
                    .build();

            restTemplate = createRestTemplate(httpClient);

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            encryptedPassword = MD5(password);
            String genratedhashKey = hashGenerator(username, senderId, message, secureKey);

            MultiValueMap<String, String> requestBodyMap = new LinkedMultiValueMap<>();

            if (!isBulk) requestBodyMap.add("mobileno", mobileNumber);
            else requestBodyMap.add("bulkmobno", mobileNumber);

            requestBodyMap.add("senderid", senderId);
            requestBodyMap.add("content", message);
            requestBodyMap.add("smsservicetype", smsServiceType);
            requestBodyMap.add("username", username);
            requestBodyMap.add("password", encryptedPassword);
            requestBodyMap.add("key", genratedhashKey);
            requestBodyMap.add("templateid", templateId);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBodyMap, httpHeaders);

            ResponseEntity<String> responseEntity = restTemplate.exchange(smsProviderURL, HttpMethod.POST, requestEntity, String.class);

            log.info(responseEntity.getBody().toString());
            responseString = responseEntity.getBody().toString();
        }
        catch (NoSuchAlgorithmException | KeyManagementException | IOException e)
        {
            log.error(e.getMessage(), e);
            throw new CustomException("", "Error occurred when sending sms");
        }

        return responseString;
    }

    protected String hashGenerator(String userName, String senderId, String content, String secureKey) {
        StringBuffer finalString = new StringBuffer();
        finalString.append(userName.trim()).append(senderId.trim()).append(content.trim()).append(secureKey.trim());
        log.info("Parameters for SHA-512 : "+finalString);
        String hashGen = finalString.toString();
        StringBuffer sb = null;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
            md.update(hashGen.getBytes());
            byte byteData[] = md.digest();
            //convert the byte to hex format method 1
            sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error(e.getMessage(), e);
            throw new CustomException("", "Error occurred when generating hash");
        }
    }

    public RestTemplate createRestTemplate(HttpClient httpClient) {
        ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        return new RestTemplate(requestFactory);
    }

    private static String MD5(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] md5 = new byte[64];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md5 = md.digest();
        return convertedToHex(md5);
    }

    private static String convertedToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < data.length; i++) {
            int halfOfByte = (data[i] >>> 4) & 0x0F;
            int twoHalfBytes = 0;

            do {
                if ((0 <= halfOfByte) && (halfOfByte <= 9)) {
                    buf.append((char) ('0' + halfOfByte));
                } else {
                    buf.append((char) ('a' + (halfOfByte - 10)));
                }

                halfOfByte = data[i] & 0x0F;

            } while (twoHalfBytes++ < 1);
        }
        return buf.toString();
    }
}
