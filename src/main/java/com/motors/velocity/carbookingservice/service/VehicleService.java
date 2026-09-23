package com.motors.velocity.carbookingservice.service;

import com.motors.velocity.carbookingservice.dto.MockVehicle;
import com.motors.velocity.carbookingservice.exception.BusinessValidationException;
import com.motors.velocity.carbookingservice.model.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final ObjectMapper objectMapper;

    private Set<String> validVehicleIds;

    @PostConstruct
    void loadVehicles() throws IOException {

        ClassPathResource resource = new ClassPathResource("static/mock/vehicles.json");

        List<MockVehicle> vehicles =
                objectMapper.readValue(resource.getInputStream(), new TypeReference<List<MockVehicle>>() {});

        validVehicleIds = vehicles.stream().map(MockVehicle::vehicleId).collect(Collectors.toUnmodifiableSet());
    }

    public void validateVehicle(String vehicleId) {

        if (!validVehicleIds.contains(vehicleId)) {
            throw new BusinessValidationException(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found: " + vehicleId);
        }
    }
}
