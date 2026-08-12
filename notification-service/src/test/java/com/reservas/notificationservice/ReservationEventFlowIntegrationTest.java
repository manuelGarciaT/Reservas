package com.reservas.notificationservice;

import com.reservas.common.events.ReservationEvent;
import com.reservas.common.events.ReservationEventType;
import com.reservas.common.security.JwtService;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"local", "test"})
@EmbeddedKafka(partitions = 1, topics = "reservation-events")
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class ReservationEventFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private KafkaProducer<String, ReservationEvent> producer;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        producer = new KafkaProducer<>(props);
    }

    @AfterEach
    void tearDown() {
        producer.close();
    }

    @Test
    void eventoDeReservaSeConsumeYQuedaDisponibleParaElUsuario() throws Exception {
        UUID userId = UUID.randomUUID();
        ReservationEvent event = new ReservationEvent(
                UUID.randomUUID(), userId, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), Instant.now().plusSeconds(3600), ReservationEventType.CREATED, Instant.now());

        producer.send(new ProducerRecord<>("reservation-events", event.reservationId().toString(), event)).get();

        String token = jwtService.generateToken(userId, "cliente-kafka", List.of("ROLE_USER"));

        boolean found = false;
        for (int i = 0; i < 20 && !found; i++) {
            String body = mockMvc.perform(get("/notifications/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            if (body.contains(event.reservationId().toString())) {
                found = true;
            } else {
                Thread.sleep(500);
            }
        }

        Assertions.assertThat(found).isTrue();
    }
}
