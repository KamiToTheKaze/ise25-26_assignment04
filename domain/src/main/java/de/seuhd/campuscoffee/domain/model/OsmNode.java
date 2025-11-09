package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 */
@Builder
public record OsmNode(
        @NonNull Long nodeId,
        @Nullable String name,
        @Nullable String description,
        @Nullable String type,
        @Nullable String campus,
        @Nullable String street,
        @Nullable String houseNumber,
        @Nullable String postalCode,
        @Nullable String city
) {
    // The record stores OSM tags that are relevant for creating a Pos object.
}
