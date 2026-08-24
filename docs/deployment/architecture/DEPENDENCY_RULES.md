# Dependency Rules

## Core Rules

1. Cache must not depend on Agent implementation packages.
2. Retrieval must not depend on RAG implementation packages.
3. Shared tool contracts belong to neutral contract packages.
4. Infrastructure adapters may depend on core contracts.
5. Core contracts must not depend on infrastructure adapters.
6. Application services may orchestrate core capabilities.
7. Controllers must not directly depend on infrastructure implementations.
8. New bidirectional package dependencies are prohibited.