package com.reservas.resourceservice.service;

import com.reservas.resourceservice.dto.ResourceRequest;
import com.reservas.resourceservice.dto.ResourceResponse;
import com.reservas.resourceservice.exception.ResourceNotFoundException;
import com.reservas.resourceservice.model.Resource;
import com.reservas.resourceservice.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(resourceRepository);
    }

    @Test
    void create_guardaYDevuelveElRecurso() {
        ResourceRequest request = new ResourceRequest("Cancha 1", "CANCHA", 10, "Sede Centro");

        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> {
            Resource r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            r.setCreatedAt(Instant.now());
            return r;
        });

        ResourceResponse response = resourceService.create(request);

        assertThat(response.name()).isEqualTo("Cancha 1");
        assertThat(response.type()).isEqualTo("CANCHA");
        assertThat(response.capacity()).isEqualTo(10);
    }

    @Test
    void getById_lanzaExcepcionSiNoExiste() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_lanzaExcepcionSiNoExiste() {
        UUID id = UUID.randomUUID();
        when(resourceRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> resourceService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resourceRepository, never()).deleteById(any());
    }

    @Test
    void update_modificaLosCamposDelRecurso() {
        UUID id = UUID.randomUUID();
        Resource existing = Resource.builder()
                .id(id).name("Viejo").type("SALA").capacity(5).location("Sede Norte")
                .createdAt(Instant.now()).build();

        when(resourceRepository.findById(id)).thenReturn(Optional.of(existing));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceRequest request = new ResourceRequest("Nuevo", "SALA", 8, "Sede Sur");
        ResourceResponse response = resourceService.update(id, request);

        assertThat(response.name()).isEqualTo("Nuevo");
        assertThat(response.capacity()).isEqualTo(8);
        assertThat(response.location()).isEqualTo("Sede Sur");
    }
}
