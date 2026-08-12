package com.reservas.resourceservice.service;

import com.reservas.resourceservice.dto.SlotRequest;
import com.reservas.resourceservice.dto.SlotResponse;
import com.reservas.resourceservice.exception.InvalidSlotException;
import com.reservas.resourceservice.exception.SlotNotFoundException;
import com.reservas.resourceservice.model.AvailabilitySlot;
import com.reservas.resourceservice.model.SlotStatus;
import com.reservas.resourceservice.repository.AvailabilitySlotRepository;
import com.reservas.resourceservice.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilitySlotServiceTest {

    @Mock
    private AvailabilitySlotRepository slotRepository;
    @Mock
    private ResourceRepository resourceRepository;

    private AvailabilitySlotService slotService;

    @BeforeEach
    void setUp() {
        slotService = new AvailabilitySlotService(slotRepository, resourceRepository);
    }

    @Test
    void create_lanzaExcepcionSiElRecursoNoExiste() {
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.existsById(resourceId)).thenReturn(false);

        SlotRequest request = new SlotRequest(Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

        assertThatThrownBy(() -> slotService.create(resourceId, request))
                .isInstanceOf(com.reservas.resourceservice.exception.ResourceNotFoundException.class);
    }

    @Test
    void create_lanzaExcepcionSiEndTimeNoEsPosteriorAStartTime() {
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.existsById(resourceId)).thenReturn(true);

        Instant now = Instant.now();
        SlotRequest request = new SlotRequest(now, now.minusSeconds(60));

        assertThatThrownBy(() -> slotService.create(resourceId, request))
                .isInstanceOf(InvalidSlotException.class);
    }

    @Test
    void create_lanzaExcepcionSiSeSuperponeConOtroTurno() {
        UUID resourceId = UUID.randomUUID();
        when(resourceRepository.existsById(resourceId)).thenReturn(true);

        Instant start = Instant.now();
        Instant end = start.plus(1, ChronoUnit.HOURS);
        SlotRequest request = new SlotRequest(start, end);

        when(slotRepository.findByResourceIdAndStartTimeLessThanAndEndTimeGreaterThan(resourceId, end, start))
                .thenReturn(List.of(mock(AvailabilitySlot.class)));

        assertThatThrownBy(() -> slotService.create(resourceId, request))
                .isInstanceOf(InvalidSlotException.class)
                .hasMessageContaining("superpone");
    }

    @Test
    void updateStatus_bloqueaUnTurnoDisponible() {
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .id(slotId).resourceId(resourceId)
                .startTime(Instant.now()).endTime(Instant.now().plusSeconds(3600))
                .status(SlotStatus.AVAILABLE).createdAt(Instant.now())
                .build();

        when(slotRepository.findByIdAndResourceId(slotId, resourceId)).thenReturn(Optional.of(slot));
        when(slotRepository.save(any(AvailabilitySlot.class))).thenAnswer(inv -> inv.getArgument(0));

        SlotResponse response = slotService.updateStatus(resourceId, slotId, SlotStatus.BLOCKED);

        assertThat(response.status()).isEqualTo(SlotStatus.BLOCKED);
    }

    @Test
    void updateStatus_lanzaExcepcionSiYaEstaBloqueado() {
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        AvailabilitySlot slot = AvailabilitySlot.builder()
                .id(slotId).resourceId(resourceId)
                .startTime(Instant.now()).endTime(Instant.now().plusSeconds(3600))
                .status(SlotStatus.BLOCKED).createdAt(Instant.now())
                .build();

        when(slotRepository.findByIdAndResourceId(slotId, resourceId)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> slotService.updateStatus(resourceId, slotId, SlotStatus.BLOCKED))
                .isInstanceOf(InvalidSlotException.class);

        verify(slotRepository, never()).save(any());
    }

    @Test
    void updateStatus_lanzaExcepcionSiElTurnoNoExiste() {
        UUID resourceId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(slotRepository.findByIdAndResourceId(slotId, resourceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> slotService.updateStatus(resourceId, slotId, SlotStatus.BLOCKED))
                .isInstanceOf(SlotNotFoundException.class);
    }
}
