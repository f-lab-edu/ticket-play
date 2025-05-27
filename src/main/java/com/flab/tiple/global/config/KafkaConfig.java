package com.flab.tiple.global.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.flab.tiple.ticket.reservation.application.dto.message.TicketReservationMessage;
import com.flab.tiple.ticket.reservation.application.dto.message.TicketReservationResultMessage;

/**
 * kafka연결 테스트시 작성한 코드로 추 후 수정될 확률 높음
 */
@Configuration
@EnableKafka
public class KafkaConfig {

	@Value("${spring.kafka.bootstrap-servers}")
	private String bootstrapServers;

	@Value("${spring.kafka.consumer.group-id}")
	private String groupId;

	@Value("${ticketing.kafka.topics.ticket-request}")
	private String ticketRequestTopic;

	@Value("${ticketing.kafka.topics.ticket-result}")
	private String ticketResultTopic;

	// Admin Config (토픽 생성용)
	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		System.out.println("kafka bootstrap servers:"+ bootstrapServers);
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		return new KafkaAdmin(configs);
	}

	// 토픽 생성 빈
	@Bean
	public NewTopic ticketRequestTopic() {
		return new NewTopic(ticketRequestTopic, 1, (short) 1);
	}

	@Bean
	public NewTopic ticketResultTopic() {
		return new NewTopic(ticketResultTopic, 1, (short) 1);
	}

	// Producer 설정
	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
		configProps.put(JsonSerializer.TYPE_MAPPINGS,
			"ticketRequest:com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationMessage," +
				"ticketResult:com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationResultMessage");

		return new DefaultKafkaProducerFactory<>(configProps);
	}

	@Bean
	public KafkaTemplate<String, Object> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	// Consumer 설정
	@Bean
	public ConsumerFactory<String, Object> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.ticketing.kafka");
		props.put(JsonDeserializer.TYPE_MAPPINGS,
			"ticketRequest:com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationMessage," +
				"ticketResult:com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationResultMessage");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationMessage");

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory());
		return factory;
	}

	// TicketReservationMessage용 특정 컨테이너 팩토리
	@Bean
	public ConsumerFactory<String, TicketReservationMessage> ticketRequestConsumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flab.tiple.ticket.reservation.dto.message.");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationMessage");

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, TicketReservationMessage> ticketRequestListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, TicketReservationMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(ticketRequestConsumerFactory());
		return factory;
	}

	// TicketReservationResultMessage용 특정 컨테이너 팩토리
	@Bean
	public ConsumerFactory<String, TicketReservationResultMessage> ticketResultConsumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flab.tiple.ticket.reservation.dto.message.");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationResultMessage");

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, TicketReservationResultMessage> ticketResultListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, TicketReservationResultMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(ticketResultConsumerFactory());
		return factory;
	}
}