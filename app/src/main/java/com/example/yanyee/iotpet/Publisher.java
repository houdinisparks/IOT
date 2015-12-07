package com.example.yanyee.iotpet;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Publisher {

    public static final String BROKER_URL = "tcp://broker.mqttdashboard.com:1883";

    public static final String TOPIC_BRIGHTNESS = "home/yousuck";
    public static final String TOPIC_TEMPERATURE = "home/sucksuck";

    private MqttClient client;


    public Publisher() {

        //We have to generate a unique Client id.
        String clientId = getMACAddress() + "-pub";


        try {

            client = new MqttClient(BROKER_URL, clientId);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);

        }
    }

    private void start() {

        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false);
            options.setWill(client.getTopic("home/LWT"), "I'm gone :(".getBytes(), 0, false);

            client.connect(options);

            //Publish data forever
            while (true) {

                publishBrightness();

                Thread.sleep(500);

                publishTemperature();

                Thread.sleep(500);
            }
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void publishTemperature() throws MqttException {
        final MqttTopic temperatureTopic = client.getTopic(TOPIC_TEMPERATURE);

        final int temperatureNumber = randomWitRange(20, 30);
        final String temperature = temperatureNumber + "°C";

        temperatureTopic.publish(new MqttMessage(temperature.getBytes()));

        System.out.println("Published data. Topic: " + temperatureTopic.getName() + "  Message: " + temperature);
    }

    private void publishBrightness() throws MqttException {
        final MqttTopic brightnessTopic = client.getTopic(TOPIC_BRIGHTNESS);

        final int brightnessNumber = randomWitRange(20, 30);
        final String brigthness = brightnessNumber + "%";

        brightnessTopic.publish(new MqttMessage(brigthness.getBytes()));

        System.out.println("Published data. Topic: " + brightnessTopic.getName() + "   Message: " + brigthness);
    }

    public static void main(String... args) {
        final Publisher publisher = new Publisher();
        publisher.start();
    }

    public static String getMACAddress() {
        InetAddress ip;

        StringBuilder sb = new StringBuilder();
        try {

            ip = InetAddress.getLocalHost();
            System.out.println("Current IP address : " + ip.getHostAddress());

            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            byte[] mac = network.getHardwareAddress();

            System.out.print("Current MAC address : ");

            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            System.out.println(sb.toString());

        } catch (UnknownHostException e) {

            e.printStackTrace();

        } catch (SocketException e) {

            e.printStackTrace();

        }
        return sb.toString();
    }

    public int randomWitRange(int min, int max) {
        int range = (max - min) + 1;
        return (int) (Math.random() * range) + min;
    }

}
