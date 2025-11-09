package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        // TODO: Implement the actual conversion (the response is currently hard-coded).
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
        // Basic required fields
        String name = osmNode.name();
        String street = osmNode.street();
        String houseNumber = osmNode.houseNumber();
        String postalCodeStr = osmNode.postalCode();
        String city = osmNode.city();

        if (name == null || street == null || houseNumber == null || postalCodeStr == null || city == null) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Parse postal code to integer
        Integer postalCode;
        try {
            postalCode = Integer.parseInt(postalCodeStr.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId());
        }

        // Determine PosType from OSM type/tag heuristics
        PosType posType = determinePosType(osmNode);

        // Determine CampusType - use campus tag if provided, otherwise default to ALTSTADT
        CampusType campus = determineCampus(osmNode);

        String description = osmNode.description() == null ? "" : osmNode.description();

        return Pos.builder()
                .name(name)
                .description(description)
                .type(posType)
                .campus(campus)
                .street(street)
                .houseNumber(houseNumber)
                .postalCode(postalCode)
                .city(city)
                .build();
    }

    private @NonNull PosType determinePosType(@NonNull OsmNode osmNode) {
        String typeTag = osmNode.type();
        String name = osmNode.name() == null ? "" : osmNode.name().toLowerCase();
        if (typeTag != null) {
            String t = typeTag.toLowerCase();
            if (t.contains("bakery") || t.contains("baker")) return PosType.BAKERY;
            if (t.contains("vending")) return PosType.VENDING_MACHINE;
            if (t.contains("caf")) return PosType.CAFE;
            if (t.contains("cafeteria") || t.contains("mensa")) return PosType.CAFETERIA;
        }

        // Fallback heuristics based on name
        if (name.contains("bäck") || name.contains("bakery") || name.contains("baker")) return PosType.BAKERY;
        if (name.contains("automat") || name.contains("vending")) return PosType.VENDING_MACHINE;
        if (name.contains("mensa") || name.contains("cafeteria")) return PosType.CAFETERIA;

        // Default
        return PosType.CAFE;
    }

    private @NonNull CampusType determineCampus(@NonNull OsmNode osmNode) {
        String campusTag = osmNode.campus();
        if (campusTag != null) {
            String t = campusTag.toUpperCase();
            try {
                return CampusType.valueOf(t);
            } catch (IllegalArgumentException ignored) {
                // ignore and fallthrough to default
            }
        }
        // Default campus when unknown
        return CampusType.ALTSTADT;
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
