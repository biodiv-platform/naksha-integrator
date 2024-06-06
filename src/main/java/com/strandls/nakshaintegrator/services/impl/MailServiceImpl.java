package com.strandls.nakshaintegrator.services.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.strandls.authentication_utility.util.PropertyFileUtil;
import com.strandls.mail_utility.model.EnumModel.DOWNLOAD_MAIL;
import com.strandls.mail_utility.model.EnumModel.FIELDS;
import com.strandls.mail_utility.model.EnumModel.INFO_FIELDS;
import com.strandls.mail_utility.model.EnumModel.MAIL_TYPE;
import com.strandls.mail_utility.producer.RabbitMQProducer;
import com.strandls.mail_utility.util.JsonUtil;
import com.strandls.nakshaintegrator.RabbitMqConnection;
import com.strandls.nakshaintegrator.services.MailService;
import com.strandls.user.controller.UserServiceApi;
import com.strandls.user.pojo.User;

public class MailServiceImpl implements MailService {
	private final Logger logger = LoggerFactory.getLogger(MailServiceImpl.class);

	@Inject
	private Channel channel;

	@Inject
	private UserServiceApi userServiceApi;

	@Override
	public void sendMail(String authorId, String fileName, String type) {
		try {
			User user = userServiceApi.getUser(authorId);

			Map<String, Object> data = new HashMap<>();
			data.put(FIELDS.TO.getAction(), new String[] { user.getEmail() });
			data.put(FIELDS.SUBSCRIPTION.getAction(), user.getSendNotification());
			Map<String, Object> model = new HashMap<>();
			model.put(DOWNLOAD_MAIL.SERVER_URL.getAction(),
					PropertyFileUtil.fetchProperty("config.properties", "serverUrl"));
			model.put(DOWNLOAD_MAIL.SITENAME.getAction(),
					PropertyFileUtil.fetchProperty("config.properties", "siteName"));
			model.put(DOWNLOAD_MAIL.USER_DATA.getAction(), user);
			model.put(DOWNLOAD_MAIL.DOWNLOAD_TYPE.getAction(), type);
			model.put(DOWNLOAD_MAIL.DOWNLOAD_FILE.getAction(), fileName);
			model.put(DOWNLOAD_MAIL.TYPE.getAction(), MAIL_TYPE.DOWNLOAD_MAIL.getAction());
			data.put(FIELDS.DATA.getAction(), JsonUtil.unflattenJSON(model));

			Map<String, Object> mData = new HashMap<>();
			mData.put(INFO_FIELDS.TYPE.getAction(), MAIL_TYPE.DOWNLOAD_MAIL.getAction());
			mData.put(INFO_FIELDS.RECIPIENTS.getAction(), Arrays.asList(data));
			RabbitMQProducer producer = new RabbitMQProducer(channel);
			if (user.getEmail() != null && !user.getEmail().isEmpty()) {
				producer.produceMail(RabbitMqConnection.EXCHANGE_BIODIV, RabbitMqConnection.MAIL_ROUTING_KEY, null,
						JsonUtil.mapToJSON(mData));
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

}
