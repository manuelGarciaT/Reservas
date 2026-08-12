package com.reservas.resourceservice.service;

import com.reservas.resourceservice.dto.ResourceRequest;
import com.reservas.resourceservice.dto.ResourceResponse;
import com.reservas.resourceservice.exception.ResourceNotFoundException;
import com.reservas.resourceservice.model.Resource;
import com.reservas.resourceservice.repository.ResourceRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.name())
                .type(request.type())
                .capacity(request.capacity())
                .location(request.location())
                .build();

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    public ResourceResponse getById(UUID id) {
        return ResourceResponse.from(findOrThrow(id));
    }

    public Page<ResourceResponse> search(String type, String location, Pageable pageable) {
        Specification<Resource> spec = buildSpecification(type, location);
        return resourceRepository.findAll(spec, pageable).map(ResourceResponse::from);
    }

    @Transactional
    public ResourceResponse update(UUID id, ResourceRequest request) {
        Resource resource = findOrThrow(id);
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setLocation(request.location());
        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(UUID id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Recurso no encontrado: " + id);
        }
        resourceRepository.deleteById(id);
    }

    private Resource findOrThrow(UUID id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado: " + id));
    }

    private Specification<Resource> buildSpecification(String type, String location) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(type)) {
                predicates.add(cb.equal(cb.lower(root.get("type")), type.toLowerCase()));
            }
            if (StringUtils.hasText(location)) {
                predicates.add(cb.equal(cb.lower(root.get("location")), location.toLowerCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
