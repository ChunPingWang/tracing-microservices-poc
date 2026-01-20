<!--
Sync Impact Report
==================
Version change: N/A (initial) → 1.0.0
Modified principles: N/A (initial creation)
Added sections:
  - I. Test-Driven Development (TDD)
  - II. Behavior-Driven Development (BDD)
  - III. Domain-Driven Design (DDD)
  - IV. SOLID Principles
  - V. Hexagonal Architecture
  - VI. Code Quality Standards
  - Architectural Constraints (Layer Rules)
  - Development Workflow (Quality Gates)
Removed sections: N/A
Templates requiring updates:
  - .specify/templates/plan-template.md: ✅ updated (Constitution Check aligned)
  - .specify/templates/spec-template.md: ✅ compatible
  - .specify/templates/tasks-template.md: ✅ compatible
Follow-up TODOs: None
==================
-->

# Weather Observability POC Constitution

## Core Principles

### I. Test-Driven Development (TDD) (NON-NEGOTIABLE)

All production code MUST be written following the TDD cycle:

1. **Red**: Write a failing test that defines expected behavior BEFORE writing implementation
2. **Green**: Write the minimum code necessary to make the test pass
3. **Refactor**: Improve code structure while keeping tests green

Non-negotiable rules:
- Tests MUST be written and MUST fail before implementation begins
- Each test MUST verify a single behavior or requirement
- Test names MUST clearly describe the expected behavior
- Unit tests MUST be isolated with no external dependencies (use test doubles)
- Code coverage targets: minimum 80% line coverage, 100% for domain layer

### II. Behavior-Driven Development (BDD)

All user-facing features MUST be specified using BDD practices:

- User stories MUST follow the format: "As a [role], I want [capability], so that [benefit]"
- Acceptance criteria MUST use Given-When-Then syntax
- Feature specifications MUST be written before implementation planning
- Integration tests MUST validate acceptance scenarios end-to-end

Scenario format:
```gherkin
Given [initial context/state]
When [action is performed]
Then [expected outcome is observed]
```

### III. Domain-Driven Design (DDD)

The domain layer is the heart of the system and MUST:

- Contain all business logic and rules
- Use Ubiquitous Language aligned with business stakeholders
- Define clear Bounded Contexts with explicit boundaries
- Implement domain concepts as:
  - **Entities**: Objects with identity that persists over time
  - **Value Objects**: Immutable objects defined by their attributes
  - **Aggregates**: Clusters of entities and value objects with a root entity
  - **Domain Services**: Stateless operations that don't belong to entities
  - **Domain Events**: Records of significant business occurrences

Domain layer MUST NOT:
- Depend on infrastructure concerns (databases, frameworks, external services)
- Contain technical implementation details
- Import from application or infrastructure layers

### IV. SOLID Principles

All code MUST adhere to SOLID principles:

- **Single Responsibility (SRP)**: Each class/module MUST have exactly one reason to change
- **Open/Closed (OCP)**: Code MUST be open for extension, closed for modification
- **Liskov Substitution (LSP)**: Subtypes MUST be substitutable for their base types
- **Interface Segregation (ISP)**: Clients MUST NOT depend on interfaces they don't use
- **Dependency Inversion (DIP)**: High-level modules MUST NOT depend on low-level modules;
  both MUST depend on abstractions

### V. Hexagonal Architecture (Ports & Adapters)

The system MUST follow hexagonal (ports & adapters) architecture with three layers:

```
┌─────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                      │
│   (Frameworks, Databases, External Services, UI, CLI)       │
│   ┌─────────────────────────────────────────────────────┐   │
│   │               Application Layer                      │   │
│   │   (Use Cases, Application Services, Orchestration)  │   │
│   │   ┌─────────────────────────────────────────────┐   │   │
│   │   │            Domain Layer                      │   │   │
│   │   │   (Entities, Value Objects, Domain Logic)   │   │   │
│   │   └─────────────────────────────────────────────┘   │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

**Layer Rules**:

| From Layer      | Can Access          | Via                    |
|-----------------|---------------------|------------------------|
| Infrastructure  | Application, Domain | Direct import          |
| Application     | Domain              | Direct import          |
| Application     | Infrastructure      | Interface (Port) ONLY  |
| Domain          | Infrastructure      | Interface (Port) ONLY  |
| Domain          | Application         | NEVER                  |

**Ports (Interfaces)**:
- Defined in the domain or application layer
- Describe WHAT the system needs, not HOW it's implemented
- Named by business capability (e.g., `WeatherDataRepository`, `NotificationSender`)

**Adapters (Implementations)**:
- Located in the infrastructure layer
- Implement port interfaces
- Named by technology (e.g., `PostgresWeatherRepository`, `EmailNotificationSender`)

### VI. Code Quality Standards

All code MUST meet these quality standards:

**Readability**:
- Code MUST be self-documenting with clear naming
- Comments MUST explain "why", not "what"
- Functions MUST be small (< 20 lines preferred, < 50 lines maximum)
- Cyclomatic complexity MUST NOT exceed 10 per function

**Maintainability**:
- No code duplication (DRY principle)
- No dead code or commented-out code in commits
- Consistent formatting enforced by linters
- All dependencies MUST be explicitly declared

**Security**:
- No secrets in code or version control
- Input validation at system boundaries
- Output encoding to prevent injection attacks
- Principle of least privilege for all operations

## Architectural Constraints

### Layer Separation Rules

**Directory Structure** (MUST follow):
```
src/
├── domain/           # Inner layer - business logic
│   ├── entities/
│   ├── value_objects/
│   ├── services/
│   ├── events/
│   └── ports/        # Interfaces for external dependencies
├── application/      # Middle layer - use cases
│   ├── use_cases/
│   ├── services/
│   ├── dto/          # Data Transfer Objects
│   └── ports/        # Interfaces for infrastructure
└── infrastructure/   # Outer layer - frameworks & tools
    ├── adapters/     # Port implementations
    ├── persistence/
    ├── external/
    ├── web/
    └── cli/
```

### Data Mapping Requirements

Data MUST be transformed via mappers when crossing layer boundaries:

- **Infrastructure → Application**: Raw data → DTO via mapper
- **Application → Domain**: DTO → Domain Entity/Value Object via mapper
- **Domain → Application**: Domain Entity → DTO via mapper
- **Application → Infrastructure**: DTO → External format via mapper

Mapper rules:
- Each mapper MUST be a separate, testable unit
- Mappers MUST NOT contain business logic
- Mappers MUST handle null/missing data gracefully
- Mappers MUST validate data structure during transformation

## Development Workflow

### Quality Gates

All code changes MUST pass these gates before merge:

1. **Pre-commit**:
   - Linting passes with zero warnings
   - Formatting is correct
   - No secrets detected

2. **Test Gate**:
   - All unit tests pass
   - All integration tests pass
   - Code coverage meets threshold (80% minimum)

3. **Review Gate**:
   - Code review by at least one team member
   - Architecture compliance verified
   - Test quality reviewed (not just coverage)

4. **Pre-merge**:
   - All CI checks pass
   - No merge conflicts
   - Branch is up to date with main

### Testing Pyramid

Tests MUST follow the testing pyramid distribution:

```
        /\
       /  \     E2E Tests (< 10%)
      /────\    - Critical user journeys only
     /      \
    /────────\  Integration Tests (20-30%)
   /          \ - API contracts, DB operations
  /────────────\
 /              \ Unit Tests (60-70%)
/________________\ - All business logic, mappers
```

## Governance

### Amendment Process

1. Propose changes via documented RFC (Request for Comments)
2. Review period: minimum 3 working days
3. Approval required from technical lead
4. Migration plan MUST be provided for breaking changes
5. All affected code MUST be updated before amendment takes effect

### Compliance Verification

- All PRs MUST include a Constitution compliance checklist
- Architecture Decision Records (ADRs) MUST reference relevant principles
- Quarterly reviews MUST assess adherence to principles
- Violations MUST be documented and addressed within one sprint

### Versioning Policy

This constitution follows semantic versioning:
- **MAJOR**: Backward-incompatible principle changes or removals
- **MINOR**: New principles added or existing principles expanded
- **PATCH**: Clarifications, typo fixes, non-semantic changes

**Version**: 1.0.0 | **Ratified**: 2026-01-20 | **Last Amended**: 2026-01-20
