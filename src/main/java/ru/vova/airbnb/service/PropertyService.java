package ru.vova.airbnb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.vova.airbnb.controller.dto.PropertyCreateRequest;
import ru.vova.airbnb.controller.dto.PropertyResponse;
import ru.vova.airbnb.exception.BookingException;
import ru.vova.airbnb.property.entity.Property;
import ru.vova.airbnb.property.repository.PropertyRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final TransactionTemplate transactionTemplate;

    @PreAuthorize("hasAuthority('PROPERTY_CREATE')")
    public PropertyResponse createProperty(PropertyCreateRequest request, Long hostId) {
        return transactionTemplate.execute(status -> {
            Property property = new Property();
            property.setHostId(hostId);
            property.setTitle(request.getTitle());
            property.setAddress(request.getAddress());
            property.setBasePricePerDay(request.getBasePricePerDay());
            property.setActive(true);

            Property saved = propertyRepository.save(property);
            return toResponse(saved);
        });
    }

    @PreAuthorize("hasAuthority('PROPERTY_VIEW_HOST_LIST')")
    public List<PropertyResponse> getHostProperties(Long hostId) {
        return propertyRepository.findByHostId(hostId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('PROPERTY_VIEW')")
    public List<PropertyResponse> getAvailableProperties() {
        return propertyRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @PreAuthorize("hasAuthority('PROPERTY_VIEW')")
    public PropertyResponse getPropertyById(Long propertyId) {
        return toResponse(findPropertyById(propertyId));
    }

    public Property findPropertyById(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> new BookingException("Property not found with id: " + propertyId));
    }

    public Property lockPropertyForUpdate(Long propertyId) {
        return propertyRepository.findByIdForUpdate(propertyId)
                .orElseThrow(() -> new BookingException("Property not found with id: " + propertyId));
    }

    public Property createProbeProperty(Long hostId, String probeKey) {
        Property property = new Property();
        property.setHostId(hostId);
        property.setTitle("TX_PROBE_" + probeKey);
        property.setAddress("TX_PROBE_ADDRESS_" + probeKey);
        property.setBasePricePerDay(java.math.BigDecimal.ONE);
        property.setActive(true);
        return propertyRepository.save(property);
    }

    private PropertyResponse toResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .hostId(property.getHostId())
                .title(property.getTitle())
                .address(property.getAddress())
            .basePricePerDay(property.getBasePricePerDay())
                .active(property.getActive())
                .createdAt(property.getCreatedAt())
                .updatedAt(property.getUpdatedAt())
                .build();
    }
}

