package com.itercraft.api.infrastructure.mqtt;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Properties;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Configuration
@ConditionalOnProperty(name = "mqtt.broker-url")
public class MqttConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttConfig.class);

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.trust-all-certificates:false}")
    private boolean trustAllCertificates;

    private final MqttSensorListener mqttSensorListener;
    private MqttClient mqttClient;

    public MqttConfig(MqttSensorListener mqttSensorListener) {
        this.mqttSensorListener = mqttSensorListener;
    }

    @PostConstruct
    public void connect() {
        try {
            doConnect();
        } catch (Exception e) {
            LOGGER.error("MQTT connection failed at startup (will retry via auto-reconnect): {}", e.getMessage());
        }
    }

    private void doConnect() throws MqttException {
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setUserName(username);
        options.setPassword(password.getBytes());
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        options.setConnectionTimeout(30);

        if (trustAllCertificates) {
            options.setSocketFactory(createTrustAllSocketFactory());
            Properties sslProps = new Properties();
            sslProps.setProperty("com.ibm.ssl.trustManager", "TrustAllCertificates");
            options.setSSLProperties(sslProps);
            options.setHttpsHostnameVerificationEnabled(false);
        }

        mqttClient = new MqttClient(brokerUrl, clientId);
        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse response) {
                LOGGER.warn("MQTT disconnected: {}", response.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                LOGGER.error("MQTT error", exception);
            }

            @Override
            public void messageArrived(String messageTopic, MqttMessage message) {
                String text = new String(message.getPayload());
                LOGGER.info("MQTT message on {}: {}", messageTopic, text);
                mqttSensorListener.handleMessage(text);
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                // Outbound only
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOGGER.info("MQTT {} to {}", reconnect ? "reconnected" : "connected", serverURI);
                try {
                    mqttClient.subscribe(topic, 1);
                    LOGGER.info("MQTT subscribed to {}", topic);
                } catch (MqttException e) {
                    LOGGER.error("MQTT subscribe failed", e);
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Not used
            }
        });

        mqttClient.connect(options);
    }

    @PreDestroy
    public void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                LOGGER.info("MQTT disconnected");
            } catch (MqttException e) {
                LOGGER.warn("MQTT disconnect error", e);
            }
        }
    }

    @SuppressWarnings("java:S4830") // Trust-all is intentional for local dev with self-signed certs
    private static javax.net.ssl.SSLSocketFactory createTrustAllSocketFactory() {
        try {
            TrustManager[] trustManagers = {new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // Trust all for dev
                }
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    // Trust all for dev
                }
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create trust-all SSLSocketFactory", e);
        }
    }
}
