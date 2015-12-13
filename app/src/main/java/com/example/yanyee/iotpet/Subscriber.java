package com.example.yanyee.iotpet;

/**
 * Created by yanyee on 12/7/2015.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;


/**
 * @author Dominik Obermaier
 * @author Christian Götz
 */
public class Subscriber {

    //brokerHostName
    public static final String BROKER_URL = "tcp://broker.mqttdashboard.com:1883";

    //We have to generate a unique Client id.
    private String clientId = Publisher.getMACAddress() + "-sub";
    private MqttClient mqttClient;

    public Subscriber(String brokerURL , String clientID) {

        try {
            mqttClient = new MqttClient(brokerURL, clientID);
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public Subscriber() {

        try {
            System.out.println(clientId);
            mqttClient = new MqttClient(BROKER_URL, clientId);
            System.out.println("client Created");

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        try {

            mqttClient.setCallback(new SubscribeCallback());
            mqttClient.connect();
            //Subscribe to all subtopics of home
            final String topic = "IOTPET/#";
            mqttClient.subscribe(topic);
            System.out.println("Subscriber is now listening to "+topic);

        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String... args) {
        final Subscriber subscriber = new Subscriber();
        subscriber.start();
    }




}