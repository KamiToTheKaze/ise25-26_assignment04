package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * OSM import service.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {

    private static final String OSM_NODE_URL = "https://api.openstreetmap.org/api/0.6/node/";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from {}", nodeId, OSM_NODE_URL);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OSM_NODE_URL + nodeId))
                    .GET()
                    .header("Accept", "application/xml")
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            int status = response.statusCode();
            if (status == 404) {
                throw new OsmNodeNotFoundException(nodeId);
            }
            if (status < 200 || status >= 300) {
                log.error("Failed to fetch OSM node {}: HTTP {}", nodeId, status);
                throw new OsmNodeNotFoundException(nodeId);
            }

            byte[] bodyBytes = response.body();
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(new ByteArrayInputStream(bodyBytes));

            // Find node element
            NodeList nodeElements = doc.getElementsByTagName("node");
            if (nodeElements.getLength() == 0) {
                throw new OsmNodeNotFoundException(nodeId);
            }

            Element nodeElem = (Element) nodeElements.item(0);

            // Parse tags
            NodeList tagList = nodeElem.getElementsByTagName("tag");
            Map<String, String> tags = new HashMap<>();
            for (int i = 0; i < tagList.getLength(); i++) {
                Element tag = (Element) tagList.item(i);
                String k = tag.getAttribute("k");
                String v = tag.getAttribute("v");
                if (k != null && !k.isEmpty()) {
                    tags.put(k, v);
                }
            }

            // Map tags to OsmNode fields
            String name = tags.getOrDefault("name", null);
            String description = tags.getOrDefault("description", null);
            String type = null;
            // prefer well-known keys for type/amenity/shop
            if (tags.containsKey("amenity")) type = tags.get("amenity");
            else if (tags.containsKey("shop")) type = tags.get("shop");
            else if (tags.containsKey("type")) type = tags.get("type");

            String campus = tags.getOrDefault("campus", null);
            String street = tags.getOrDefault("addr:street", null);
            String houseNumber = tags.getOrDefault("addr:housenumber", null);
            String postalCode = tags.getOrDefault("addr:postcode", null);
            String city = tags.getOrDefault("addr:city", null);

            return OsmNode.builder()
                    .nodeId(nodeId)
                    .name(name)
                    .description(description)
                    .type(type)
                    .campus(campus)
                    .street(street)
                    .houseNumber(houseNumber)
                    .postalCode(postalCode)
                    .city(city)
                    .build();

        } catch (OsmNodeNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching/parsing OSM node {}: {}", nodeId, e.getMessage(), e);
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
}
