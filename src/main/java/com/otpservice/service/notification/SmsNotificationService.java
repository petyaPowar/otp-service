package com.otpservice.service.notification;

import com.otpservice.config.AppConfig;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class SmsNotificationService implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(SmsNotificationService.class);
    private static final TimeFormatter TIME_FORMATTER = new AbsoluteTimeFormatter();

    private final String host;
    private final int port;
    private final String systemId;
    private final String password;
    private final String systemType;
    private final String sourceAddr;
    private final TypeOfNumber sourceTon;
    private final NumberingPlanIndicator sourceNpi;
    private final TypeOfNumber destTon;
    private final NumberingPlanIndicator destNpi;

    public SmsNotificationService() {
        Properties p = AppConfig.getInstance().getSmsProps();
        this.host = p.getProperty("smpp.host", "localhost");
        this.port = Integer.parseInt(p.getProperty("smpp.port", "2775"));
        this.systemId = p.getProperty("smpp.system_id", "smppclient1");
        this.password = p.getProperty("smpp.password", "password");
        this.systemType = p.getProperty("smpp.system_type", "");
        this.sourceAddr = p.getProperty("smpp.source_addr", "OTPService");
        this.sourceTon = TypeOfNumber.valueOf(Byte.parseByte(p.getProperty("smpp.source_addr_ton", "5")));
        this.sourceNpi = NumberingPlanIndicator.valueOf(Byte.parseByte(p.getProperty("smpp.source_addr_npi", "0")));
        this.destTon = TypeOfNumber.valueOf(Byte.parseByte(p.getProperty("smpp.dest_addr_ton", "1")));
        this.destNpi = NumberingPlanIndicator.valueOf(Byte.parseByte(p.getProperty("smpp.dest_addr_npi", "1")));
    }

    @Override
    public void send(String destination, String code, String operationId) throws Exception {
        SMPPSession session = new SMPPSession();
        try {
            session.connectAndBind(host, port, new BindParameter(
                    BindType.BIND_TX,
                    systemId, password, systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    null
            ));
            String text = "OTP for [" + operationId + "]: " + code;
            session.submitShortMessage(
                    systemType,
                    sourceTon, sourceNpi, sourceAddr,
                    destTon, destNpi, destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    TIME_FORMATTER.format((java.util.Date) null),
                    TIME_FORMATTER.format((java.util.Date) null),
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT, MessageClass.CLASS1, false),
                    (byte) 0,
                    text.getBytes(StandardCharsets.UTF_8)
            );
            log.info("OTP sent via SMS to {} for operation {}", destination, operationId);
        } finally {
            session.unbindAndClose();
        }
    }
}
