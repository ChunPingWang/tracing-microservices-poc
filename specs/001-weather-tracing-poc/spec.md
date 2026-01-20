# Feature Specification: Weather Tracing PoC

**Feature Branch**: `001-weather-tracing-poc`
**Created**: 2026-01-20
**Status**: Draft
**Input**: PRD.md - Weather Tracing PoC distributed system observability demonstration

## Clarifications

### Session 2026-01-21

- Q: Should weather data be stored in database or generated fresh per request? → A: Store in database - City baselines stored; each query reads from DB and applies random variation
- Q: What language should the frontend UI use? → A: Chinese (Traditional) - UI follows PRD mockup with Chinese labels; technical terms (Trace ID) in English
- Q: 確認每次查詢氣溫與天氣是否不同? → A: 已確認 - FR-010 規定每次查詢加入隨機變化，以便外部查詢可觀察不同的鏈路追蹤變化

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Query City Weather (Priority: P1)

As a user viewing the weather demonstration system, I want to select one of three cities (Taipei, Taichung, Kaohsiung) and view the current weather information (temperature and rainfall), so that I can see the system functioning and generating traceable requests.

**Why this priority**: This is the core functionality that generates the distributed requests needed for observability demonstration. Without weather queries, there is nothing to trace or monitor.

**Independent Test**: Can be fully tested by clicking any city button and verifying weather data appears with temperature and rainfall values. Delivers immediate value as the primary user interaction.

**Acceptance Scenarios**:

1. **Given** the weather query page is loaded, **When** I view the interface, **Then** I see three city buttons: Taipei (TPE), Taichung (TXG), and Kaohsiung (KHH)
2. **Given** I am on the weather page, **When** I click the Taipei button, **Then** I see the temperature (in Celsius) and rainfall (in mm) for Taipei
3. **Given** I am on the weather page, **When** I click any city button, **Then** I see a loading indicator while the request is processing
4. **Given** I am on the weather page, **When** a request fails, **Then** I see a user-friendly error message with an option to retry
5. **Given** weather data is displayed, **When** I view the data, **Then** I see the last update timestamp for the data
6. **Given** I query the same city multiple times, **When** I compare the results, **Then** I see different temperature and/or rainfall values each time (random variation)

---

### User Story 2 - View Request Trace Information (Priority: P2)

As a developer or demonstration audience member, I want to see the Trace ID of my weather query request displayed on the page and be able to navigate directly to the tracing UI to view the complete request chain, so that I can understand how distributed tracing works.

**Why this priority**: This is the primary observability feature that demonstrates the value of distributed tracing. It depends on weather queries (P1) to generate traces.

**Independent Test**: Can be tested by making a weather query and verifying the Trace ID appears, then clicking to navigate to the tracing visualization UI and confirming the trace details are visible.

**Acceptance Scenarios**:

1. **Given** I have queried weather for a city, **When** the response is received, **Then** I see the Trace ID displayed on the page
2. **Given** the Trace ID is displayed, **When** I click on it, **Then** I am navigated to the tracing UI showing the detailed request chain
3. **Given** I have made a weather query, **When** the response is received, **Then** I see the total request duration displayed
4. **Given** I view the trace in the tracing UI, **When** I examine the trace details, **Then** I see the complete chain: Frontend to Gateway to Weather Service to Database

---

### User Story 3 - Monitor System Metrics (Priority: P3)

As a developer or operations team member, I want to view system metrics through a dashboard including request counts, latency distribution, and error rates filtered by service, so that I can understand the system's health and performance.

**Why this priority**: Metrics monitoring complements tracing by providing aggregate views. It requires the system to be generating traffic (P1) and benefits from understanding individual traces (P2).

**Independent Test**: Can be tested by accessing the metrics dashboard and verifying that request counts, latency percentiles, and error rates are displayed with the ability to filter by service.

**Acceptance Scenarios**:

1. **Given** the metrics dashboard is accessible, **When** I view the dashboard, **Then** I see the request count (QPS) metric
2. **Given** the metrics dashboard is loaded, **When** I view latency metrics, **Then** I see latency distribution (P50, P95, P99 percentiles)
3. **Given** the metrics dashboard is loaded, **When** I view error metrics, **Then** I see the error rate displayed
4. **Given** multiple services are running, **When** I use the service filter, **Then** I can view metrics for individual services separately

---

### Edge Cases

- What happens when a user queries an invalid city code not in the supported list (TPE, TXG, KHH)?
  - System returns a 404 error with message "City not found" and the Trace ID is still captured
- What happens when the backend service is temporarily unavailable?
  - Frontend shows a user-friendly error message and retry option; the failed request is still traced
- What happens when multiple rapid queries are made?
  - Each query generates a unique Trace ID; the UI shows the most recent query result
- What happens when the tracing or metrics infrastructure is down but the core service is running?
  - Weather queries continue to work; tracing information may be unavailable or show "Trace unavailable"

## Requirements *(mandatory)*

### Functional Requirements

#### Weather Query
- **FR-001**: System MUST display three city selection buttons: 台北 (TPE), 台中 (TXG), 高雄 (KHH)
- **FR-001a**: Frontend UI MUST use Chinese (Traditional) for all user-facing labels; technical terms (Trace ID, Span ID) remain in English
- **FR-002**: System MUST return weather data including city code, city name (Chinese), temperature (Celsius), rainfall (mm), and last updated timestamp for valid city queries
- **FR-003**: System MUST display a loading indicator when a weather query is in progress
- **FR-004**: System MUST display a user-friendly error message (in Chinese) when a query fails, with a retry option
- **FR-005**: System MUST return a 404 error with code "CITY_NOT_FOUND" and Chinese message "找不到指定的城市" for invalid city codes

#### Simulated Weather Data
- **FR-006**: System MUST store city baseline weather data in a database (baseline temperature and rainfall per city)
- **FR-007**: System MUST read baseline data from database on each weather query to generate authentic database spans for tracing
- **FR-008**: System MUST apply city-specific baseline temperatures: Taipei 25 degrees, Taichung 27 degrees, Kaohsiung 29 degrees
- **FR-009**: System MUST apply city-specific baseline rainfall: Taipei 15mm, Taichung 10mm, Kaohsiung 8mm
- **FR-010**: System MUST generate unique random variation on EVERY query (plus/minus 2 degrees for temperature, plus/minus 5mm for rainfall) so that consecutive queries for the same city return different values
- **FR-010a**: Final temperature MUST be clamped to range 15-35 degrees Celsius; rainfall to 0-50mm
- **FR-010b**: Each query MUST produce observably different trace characteristics (timing, data values) to demonstrate trace chain variation in the tracing UI

#### Trace Information Display
- **FR-011**: System MUST display the Trace ID in the response for every weather query (success or failure)
- **FR-012**: System MUST make the Trace ID clickable, navigating to the tracing UI with the trace details
- **FR-013**: System MUST display the request duration (in milliseconds) for each query
- **FR-014**: System MUST include X-Trace-Id, X-Span-Id, and X-Request-Duration in response headers

#### Distributed Tracing
- **FR-015**: System MUST propagate trace context across all service boundaries (Frontend, Gateway, Weather Service, Database)
- **FR-016**: Each trace MUST include spans for: Frontend to Gateway, Gateway to Weather Service, Weather Service to Database
- **FR-017**: Each span MUST include: operation name, start/end time, status (success/failure), and relevant tags (city code, service name)

#### System Metrics
- **FR-018**: System MUST expose request count metrics grouped by service and endpoint
- **FR-019**: System MUST expose request latency metrics with P50, P95, and P99 percentiles
- **FR-020**: System MUST expose error rate metrics
- **FR-021**: System MUST expose service health metrics (memory usage, connection pool status)
- **FR-022**: Metrics dashboard MUST allow filtering by service

#### Navigation and Links
- **FR-023**: System MUST provide quick links to: Tracing UI, Metrics Dashboard, and Metrics Data Source
- **FR-024**: System MUST provide health check endpoints for Gateway and Weather Service

#### Deployment
- **FR-025**: System MUST support local development mode
- **FR-026**: System MUST support container-based single-command deployment
- **FR-027**: System MUST support container orchestration deployment
- **FR-028**: System MUST provide automated deployment scripts for all deployment modes

### Key Entities

- **City**: Represents a queryable location with code (TPE/TXG/KHH), Chinese name (台北/台中/高雄) as primary display, baseline temperature, and baseline rainfall
- **WeatherData**: Represents weather information for a city with city reference, current temperature, current rainfall, and timestamp
- **TraceInfo**: Represents request tracing metadata with trace ID, span ID, and request duration
- **ApiResponse**: Represents the standardized response format with success status, data payload, error information, and trace info

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can query weather for any of the three cities and receive a response within 200 milliseconds (P95)
- **SC-002**: 100% of successful weather queries display the corresponding Trace ID on the page
- **SC-003**: 100% of Trace IDs link correctly to the tracing UI showing the complete 4-hop request chain (Frontend, Gateway, Service, Database)
- **SC-004**: Metrics dashboard displays real-time request counts with updates visible within 15 seconds of queries being made
- **SC-005**: System can support 10 concurrent users making queries without performance degradation
- **SC-006**: All four deployment modes (local, container single-command, container orchestration, automated scripts) successfully start and serve requests
- **SC-007**: Demonstration audience can complete the full demo script (query weather, view trace, view metrics) within 10 minutes
- **SC-008**: Error scenarios display user-friendly messages with Trace IDs for debugging within 1 second of failure detection
- **SC-009**: Consecutive queries for the same city return different values at least 95% of the time, demonstrating observable trace variation

## Assumptions

- Weather data is simulated (not from real weather APIs) but stored in a database to demonstrate authentic database tracing spans
- No user authentication is required; the system is open access for demonstration purposes
- The system is designed for demonstration scale (10 concurrent users) not production scale
- All three cities use simulated data with the defined baseline values and random variations
- The tracing UI and metrics dashboard are assumed to be standard observability tools accessed via web interface
