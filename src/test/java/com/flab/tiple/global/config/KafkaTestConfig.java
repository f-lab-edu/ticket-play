package com.flab.tiple.global.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import com.flab.tiple.ticket.reservation.application.dto.message.TicketReservationMessage;
import com.flab.tiple.ticket.reservation.application.dto.message.TicketReservationResultMessage;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@TestConfiguration
@EmbeddedKafka(partitions = 1, topics = {"ticket-reservation-requests", "ticket-reservation-results"})
@TestPropertySource(properties = {
	"spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
	"spring.kafka.consumer.group-id=test-group",
	"ticketing.kafka.topics.ticket-request=ticket-reservation-requests",
	"ticketing.kafka.topics.ticket-result=ticket-reservation-results"
})
@EnableKafka
public class KafkaTestConfig {

	@Autowired
	private EmbeddedKafkaBroker embeddedKafkaBroker;

	@Bean
	public KafkaAdmin kafkaAdmin() {
		Map<String, Object> configs = new HashMap<>();
		configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		return new KafkaAdmin(configs);
	}

	@Bean
	public ProducerFactory<String, Object> producerFactory() {
		Map<String, Object> configProps = new HashMap<>();
		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
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

	@Bean
	public ConsumerFactory<String, Object> consumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flab.tiple.ticket.reservation.dto.message");
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

	// 메시지별 컨테이너 팩토리 설정도 동일하게 유지
	@Bean
	public ConsumerFactory<String, TicketReservationMessage> ticketRequestConsumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flab.tiple.ticket.reservation.dto.message");
		props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.flab.tiple.ticket.reservation.application.message.dto.TicketReservationMessage");

		return new DefaultKafkaConsumerFactory<>(props);
	}

	@Bean
	public ConcurrentKafkaListenerContainerFactory<String, TicketReservationMessage> ticketRequestListenerContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<String, TicketReservationMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(ticketRequestConsumerFactory());
		return factory;
	}

	@Bean
	public ConsumerFactory<String, TicketReservationResultMessage> ticketResultConsumerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
		props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.flab.tiple.ticket.reservation.dto.message");
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