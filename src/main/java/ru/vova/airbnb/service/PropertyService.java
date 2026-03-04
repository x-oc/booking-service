package ru.vova.airbnb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.vova.airbnb.controller.dto.PropertyCreateRequest;
import ru.vova.airbnb.controller.dto.PropertyResponse;
import ru.vova.airbnb.entity.Property;
import ru.vova.airbnb.exception.BookingException;
import ru.vova.airbnb.repository.PropertyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;

    @Transactional
    @PreAuthorize("hasRole('HOST')")
    public PropertyResponse createProperty(PropertyCreateRequest request, Long hostId) {
        Property property = new Property();
        property.setHostId(hostId);
        property.setTitle(request.getTitle());
        property.setAddress(request.getAddress());
        property.setActive(true);

        Property saved = propertyRepository.save(property);
        return toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('HOST', 'ADMIN')")
    public List<PropertyResponse> getHostProperties(Long hostId) {
        return propertyRepository.findByHostId(hostId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('GUEST', 'HOST', 'ADMIN')")
    public PropertyResponse getPropertyById(Long propertyId) {
        return toResponse(findPropertyById(propertyId));
    }

    public Property findPropertyById(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BookingException("Property not found with id: " + propertyId));
    }

    @Transactional
    public Property lockPropertyForUpdate(Long propertyId) {
        return propertyRepository.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new BookingException("Property not found with id: " + propertyId));
    }

    private PropertyResponse toResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .hostId(property.getHostId())
                .title(property.getTitle())
                .address(property.getAddress())
                .active(property.getActive())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
}

