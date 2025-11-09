package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PosServiceOsmImportTest {

    @Test
    void importFromOsmNode_createsPosFromOsmNode() {
        // Arrange: create stub OsmDataService returning a populated OsmNode
        OsmDataService osmDataService = nodeId -> OsmNode.builder()
                .nodeId(nodeId)
                .name("Test Cafe")
                .description("Lovely place")
                .type("cafe")
                .campus("ALTSTADT")
                .street("Test Street")
                .houseNumber("12")
                .postalCode("69117")
                .city("Heidelberg")
                .build();

        // Stub PosDataService that returns the pos with an ID when upsert is called
        AtomicLong idCounter = new AtomicLong(100);
        PosDataService posDataService = new PosDataService() {
            @Override
            public void clear() {
            }

            @Override
            public java.util.List<de.seuhd.campuscoffee.domain.model.Pos> getAll() {
                return java.util.List.of();
            }

            @Override
            public de.seuhd.campuscoffee.domain.model.Pos getById(java.lang.Long id) {
                throw new RuntimeException();
            }

            @Override
            public de.seuhd.campuscoffee.domain.model.Pos upsert(de.seuhd.campuscoffee.domain.model.Pos pos) throws RuntimeException {
                // return the same pos with an id set
                return pos.toBuilder().id(idCounter.incrementAndGet()).build();
            }
        };

        PosServiceImpl service = new PosServiceImpl(posDataService, osmDataService);

        // Act
        Pos created = service.importFromOsmNode(123L);

        // Assert
        assertNotNull(created.id());
        assertEquals("Test Cafe", created.name());
        assertEquals("Lovely place", created.description());
        assertEquals(PosType.CAFE, created.type());
        assertEquals(CampusType.ALTSTADT, created.campus());
        assertEquals("Test Street", created.street());
        assertEquals("12", created.houseNumber());
        assertEquals(69117, created.postalCode());
        assertEquals("Heidelberg", created.city());
    }

    @Test
    void importFromOsmNode_whenOsmNodeMissingFields_throws() {
        OsmDataService osmDataService = nodeId -> OsmNode.builder().nodeId(nodeId).build();
        PosDataService posDataService = new PosDataService() {
            @Override public void clear() {}
            @Override public java.util.List<de.seuhd.campuscoffee.domain.model.Pos> getAll() { return java.util.List.of(); }
            @Override public de.seuhd.campuscoffee.domain.model.Pos getById(java.lang.Long id) { throw new RuntimeException(); }
            @Override public de.seuhd.campuscoffee.domain.model.Pos upsert(de.seuhd.campuscoffee.domain.model.Pos pos) { return pos; }
        };
        PosServiceImpl service = new PosServiceImpl(posDataService, osmDataService);
        assertThrows(RuntimeException.class, () -> service.importFromOsmNode(1L));
    }
}
