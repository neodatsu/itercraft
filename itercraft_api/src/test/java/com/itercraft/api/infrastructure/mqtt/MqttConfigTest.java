package com.itercraft.api.infrastructure.mqtt;

import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MqttConfigTest {

    @Mock
    private MqttSensorListener mqttSensorListener;

    private MqttConfig mqttConfig;

    @BeforeEach
    void setUp() {
        mqttConfig = new MqttConfig(mqttSensorListener);
        ReflectionTestUtils.setField(mqttConfig, "brokerUrl", "tcp://localhost:1883");
        ReflectionTestUtils.setField(mqttConfig, "username", "testuser");
        ReflectionTestUtils.setField(mqttConfig, "password", "testpass");
        ReflectionTestUtils.setField(mqttConfig, "clientId", "test-client");
        ReflectionTestUtils.setField(mqttConfig, "topic", "sensors/#");
        ReflectionTestUtils.setField(mqttConfig, "trustAllCertificates", false);
    }

    @Test
    void connect_shouldCreateClientAndConnect() throws MqttException {
        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> doNothing().when(mock).connect(any()))) {

            mqttConfig.connect();

            assertThat(mocked.constructed()).hasSize(1);
            MqttClient client = mocked.constructed().getFirst();
            verify(client).setCallback(any(MqttCallback.class));
            verify(client).connect(any(MqttConnectionOptions.class));
        }
    }

    @Test
    void connect_withTrustAllCertificates_shouldConfigureSsl() throws MqttException {
        ReflectionTestUtils.setField(mqttConfig, "trustAllCertificates", true);

        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> doNothing().when(mock).connect(any()))) {

            mqttConfig.connect();

            MqttClient client = mocked.constructed().getFirst();
            ArgumentCaptor<MqttConnectionOptions> captor = ArgumentCaptor.forClass(MqttConnectionOptions.class);
            verify(client).connect(captor.capture());

            MqttConnectionOptions options = captor.getValue();
            assertThat(options.getSocketFactory()).isNotNull();
            assertThat(options.getSSLProperties())
                    .containsEntry("com.ibm.ssl.trustManager", "TrustAllCertificates");
        }
    }

    @Test
    void connect_shouldNotCrashAppOnFailure() {
        try (MockedConstruction<MqttClient> ignored = mockConstruction(MqttClient.class,
                (mock, context) -> doThrow(new MqttException(1)).when(mock).connect(any()))) {

            assertThatCode(() -> mqttConfig.connect()).doesNotThrowAnyException();
        }
    }

    @Test
    void disconnect_whenConnected_shouldDisconnect() throws MqttException {
        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> {
                    doNothing().when(mock).connect(any());
                    when(mock.isConnected()).thenReturn(true);
                })) {

            mqttConfig.connect();
            mqttConfig.disconnect();

            verify(mocked.constructed().getFirst()).disconnect();
        }
    }

    @Test
    void disconnect_whenNotConnected_shouldNotDisconnect() throws MqttException {
        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> {
                    doNothing().when(mock).connect(any());
                    when(mock.isConnected()).thenReturn(false);
                })) {

            mqttConfig.connect();
            mqttConfig.disconnect();

            verify(mocked.constructed().getFirst(), never()).disconnect();
        }
    }

    @Test
    void disconnect_whenClientNull_shouldDoNothing() {
        mqttConfig.disconnect();
        // No exception
    }

    @Test
    void callback_messageArrived_shouldDelegateToListener() throws Exception {
        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);

        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> doNothing().when(mock).connect(any()))) {

            mqttConfig.connect();
            verify(mocked.constructed().getFirst()).setCallback(callbackCaptor.capture());

            MqttMessage message = new MqttMessage("{\"test\":true}".getBytes());
            callbackCaptor.getValue().messageArrived("sensors/test", message);

            verify(mqttSensorListener).handleMessage("{\"test\":true}");
        }
    }

    @Test
    void callback_connectComplete_shouldSubscribe() throws Exception {
        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);

        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> doNothing().when(mock).connect(any()))) {

            mqttConfig.connect();
            MqttClient client = mocked.constructed().getFirst();
            verify(client).setCallback(callbackCaptor.capture());

            callbackCaptor.getValue().connectComplete(false, "tcp://localhost:1883");

            verify(client).subscribe("sensors/#", 1);
        }
    }

    @Test
    void callback_disconnected_shouldLogWarning() throws Exception {
        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);

        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> doNothing().when(mock).connect(any()))) {

            mqttConfig.connect();
            verify(mocked.constructed().getFirst()).setCallback(callbackCaptor.capture());

            MqttDisconnectResponse response = new MqttDisconnectResponse(new MqttException(0));
            callbackCaptor.getValue().disconnected(response);
            // No exception
        }
    }

    @Test
    void callback_mqttErrorOccurred_shouldLogError() throws Exception {
        ArgumentCaptor<MqttCallback> callbackCaptor = ArgumentCaptor.forClass(MqttCallback.class);

        try (MockedConstruction<MqttClient> mocked = mockConstruction(MqttClient.class,
                (mock, context) -> doNothing().when(mock).connect(any()))) {

            mqttConfig.connect();
            verify(mocked.constructed().getFirst()).setCallback(callbackCaptor.capture());

            callbackCaptor.getValue().mqttErrorOccurred(new MqttException(1));
            // No exception
        }
    }
}
