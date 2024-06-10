package com.strandls.nakshaintegrator;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.strandls.authentication_utility.util.PropertyFileUtil;

public class RabbitMqConnection {
	private static final String QUEUE_ELASTIC = "elastic";
	private static final String ROUTING_ELASTIC = "esmodule";

	public static final String EXCHANGE_BIODIV;
	public static final String MAIL_QUEUE;
	public static final String MAIL_ROUTING_KEY;

	static {
		EXCHANGE_BIODIV = PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_exchange");
		MAIL_QUEUE = PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_queue");
		MAIL_ROUTING_KEY = PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_routingKey");
	}

	public Channel setRabbitMQConnetion() throws IOException, TimeoutException {
		String rabbitmqHost = PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_host");
		Integer rabbitmqPort = Integer.parseInt(PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_port"));
		String rabbitmqUsername = PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_username");
		String rabbitmqPassword = PropertyFileUtil.fetchProperty("config.properties", "rabbitmq_password");

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(rabbitmqHost);
		factory.setPort(rabbitmqPort);
		factory.setUsername(rabbitmqUsername);
		factory.setPassword(rabbitmqPassword);

		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();
		channel.exchangeDeclare(EXCHANGE_BIODIV, "direct");
		channel.queueDeclare(QUEUE_ELASTIC, false, false, false, null);
		channel.queueBind(QUEUE_ELASTIC, EXCHANGE_BIODIV, ROUTING_ELASTIC);
		channel.queueDeclare(MAIL_QUEUE, false, false, false, null);
		channel.queueBind(MAIL_QUEUE, EXCHANGE_BIODIV, MAIL_ROUTING_KEY);
		return channel;
	}
}
