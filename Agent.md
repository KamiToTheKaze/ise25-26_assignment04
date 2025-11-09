Project Requirement Proposal (PRP)
You are a senior software engineer. Use the information below to implement a new feature or improvement in this software project.

Goal
Feature Goal: Implementiere ein Feature, welches es ermöglicht einen neuen Point of Sale (POS) basierend auf einem OpenStreetMap Eintrag zu importieren. Es sollen die OSM XML Daten via node ID ausgelesen werden. Dabei sollen die relevanten Daten betrachtet werden: Name, description, type, Campus, street, houseNumber, postalCode, City. Daraufhin soll ein neuer POS Eintrag in der application Datenbank erstellt werden

Deliverable:
Neue Spring boot Service Klasse: OSMImport
Api Endpunkt in api modul api/pos/Import/osm/{nodeId}
Tests für die Funktionalität

Success Definition: Wenn eine valide OSM node ID gegeben ist, importiert das System die Informationen in die Datenbank und erstell den neuen POS Eintrag.

User Persona
Target User: Admins und Devs, die schnell mit wenig Arbeit ein neues POS einfügen wollen.

Use Case: user gibt OSM node ID eines Shops an. Daraufhin fetched das System die node XML Daten ließt die relevanten Daten und fügt ein neues POS ein.

User Journey:
1. Admin gibt ein: POST api/pos/Import/osm/{nodeID}
2. Application holt OSM node XML via https://api.openstreetmap.org/api/0.6/node/{nodeId}
3. XML extrahiert: Name -> POS Name, addr:street -> street, addr:city -> City, addr:postcode -> Postalcode, addr:housenumber -> houseNumber
4. Neue POS erstellt
5. neuer POS kann gehot werden via /api/pos/{id}


Pain Points Addressed: Kein manuelles auslesen der Daten für neue POS Einträge

Why
Einfaches hinzufügen  neuer Partner Betriebe.
Admins und devs müssen keine Einträge von Hand erstellen und sparen Arbeitszeit.
Partner Betriebe können sich darauf verlassen das die OSM Daten richtig genutzt werden.

What
Endpunkt der API: POST /api/pos/import/osm/{nodeId}
Request Node ID als path Parameter
Vorgehen:
1. Holen der OSM node Xml via POST /api/pos/import/osm/{nodeId}
2. Relevante Tags für die POS Erstellung aus der XML lesen
3. POS object mit gegeben Daten erstellen
4. Persistent machen
5. 201 Erstellt mit neuem Pos object zurückgeben
   Wenn Node Id nicht korrekt ist, gebe 404 not found zurück, bei anderen fehlern 400 error


Success Criteria
Api gibt 201 zurück wenn OSM node valide ist
Verifiziert mit echter OSM node: 5589879349

Documentation & References
MUST READ - Include the following information in your context window.

The README.md file at the root of the project contains setup instructions and example API calls.

This Java Spring Boot application is structured as a multi-module Maven project following the ports-and-adapters architectural pattern. There are the following submodules:

api - Maven submodule for controller adapter.

application - Maven submodule for Spring Boot application, test data import, and system tests.

data - Maven submodule for data adapter.

domain - Maven submodule for domain model, main business logic, and ports.