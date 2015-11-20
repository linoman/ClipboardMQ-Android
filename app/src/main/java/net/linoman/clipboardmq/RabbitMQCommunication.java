package net.linoman.clipboardmq;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public class RabbitMQCommunication {
    public static String MESSAGE = "msg";

    private static String URI = "amqp://clipman:clipman123@linoman.net/ClipboardMQ";
    private static String EXCHANGE_ID = "cmq.exchange.topic.transient";
    private static String EXCHANGE_TYPE = "topic";
    private static String ROUTING_KEY = "cmq.linoman";

    private Thread subscribeThread;
    private Thread publishThread;

    private ConnectionFactory connectionFactory = new ConnectionFactory();
    private BlockingDeque<String> queue = new LinkedBlockingDeque<>();

    private String lastMessage = null;
    private Channel channel = null;

    public RabbitMQCommunication() {
        setConnectionFactory();
    }

    private void setConnectionFactory() {
        try {
            connectionFactory.setAutomaticRecoveryEnabled(false);
            connectionFactory.setUri(URI);
        } catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {
            Log.e(RabbitMQCommunication.class.getName(), "Error while connecting with RabbitMQ server", e);
        }
    }

    public void consumer(final Handler handler) {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection connection = connectionFactory.newConnection();
                        channel = connection.createChannel();
                        channel.exchangeDeclare(EXCHANGE_ID, EXCHANGE_TYPE);

                        AMQP.Queue.DeclareOk q = channel.queueDeclare();
                        channel.queueBind(q.getQueue(), EXCHANGE_ID, ROUTING_KEY);
                        channel.queueBind(q.getQueue(), EXCHANGE_ID, "cmq.linoman.android");
                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(q.getQueue(), true, consumer);

                        while(true) {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                            String message = new String(delivery.getBody());
                            if(!message.equals(lastMessage)) {
                                Log.d(RabbitMQCommunication.class.getName(), "[r] " + message);
                                lastMessage = message;
                                Message msg = handler.obtainMessage();
                                Bundle bundle = new Bundle();
                                bundle.putString(MESSAGE, message);
                                msg.setData(bundle);
                                handler.sendMessage(msg);
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.d(RabbitMQCommunication.class.getName(), "Connection broken ", e);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }

    public void producer() {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        channel.confirmSelect();
                        while(true) {
                            String message = queue.takeFirst();
                            try {
                                channel.basicPublish(EXCHANGE_ID, ROUTING_KEY, null, message.getBytes());
                                Log.d(RabbitMQCommunication.class.getName(), "[s] " + message);
                                channel.waitForConfirms();
                            } catch (Exception e) {
                                Log.e(RabbitMQCommunication.class.getName(), "[f] " + message);
                                queue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.e(RabbitMQCommunication.class.getName(), "Connection broken " + e.getClass().getName());
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }

    public void publishMessage(String message) {
        try {
            if(!message.equals(lastMessage)) {
                Log.d(RabbitMQCommunication.class.getName(), "[q] " + message);
                queue.putLast(message);
                lastMessage = message;
            }
        } catch (InterruptedException e) {
            Log.e(RabbitMQCommunication.class.getName(), "Error while queuing message", e);
        }
    }

    public void interruptSubscriberThread() {
        subscribeThread.interrupt();
        publishThread.interrupt();
    }
}
