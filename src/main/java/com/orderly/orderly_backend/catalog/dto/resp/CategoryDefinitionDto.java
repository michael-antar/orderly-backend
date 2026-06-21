package com.orderly.orderly_backend.catalog.dto.resp;

import java.util.List;
import java.util.UUID;

public record CategoryDefinitionDto(
        UUID id,
        String name,
        String icon,
        int sortOrder,
        List<FieldDefinitionDto> fieldDefinitions
) {}
