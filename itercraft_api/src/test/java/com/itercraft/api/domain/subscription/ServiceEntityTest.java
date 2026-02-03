package com.itercraft.api.domain.subscription;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceEntityTest {

    @Test
    void getters_shouldReturnCorrectValues() throws Exception {
        ServiceEntity service = new ServiceEntity();

        UUID id = UUID.randomUUID();
        String code = "tondeuse";
        String label = "Tondeuse";
        String description = "Service de tondeuse automatique";
        OffsetDateTime createdAt = OffsetDateTime.now();

        setField(service, "id", id);
        setField(service, "code", code);
        setField(service, "label", label);
        setField(service, "description", description);
        setField(service, "createdAt", createdAt);

        assertThat(service.getId()).isEqualTo(id);
        assertThat(service.getCode()).isEqualTo(code);
        assertThat(service.getLabel()).isEqualTo(label);
        assertThat(service.getDescription()).isEqualTo(description);
        assertThat(service.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void protectedConstructor_shouldCreateEmptyEntity() {
        ServiceEntity service = new ServiceEntity();

        assertThat(service.getId()).isNull();
        assertThat(service.getCode()).isNull();
        assertThat(service.getLabel()).isNull();
        assertThat(service.getDescription()).isNull();
        assertThat(service.getCreatedAt()).isNull();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
