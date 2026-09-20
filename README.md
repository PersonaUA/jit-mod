# JIT Modules

Public modules for the JIT platform.

This repository is intentionally independent from `PersonaUA/jit-node`.

## Boundary

Module implementations must compile against published/versioned JIT Module SDK artifacts. They must not use Gradle project dependencies, source includes, filesystem links or imports from the `jit-node` source tree.

Current local development contract:

```text
ua.com.jit:module-runtime-api:0.1.0-SNAPSHOT
```

The artifact is produced by `jit-node` and consumed here through Maven Local during early development. A remote public package repository can replace Maven Local later without changing the source dependency boundary.

## Modules

### probe

Minimal external Module used to prove the runtime contract.

It intentionally has:

- no UI;
- no storage;
- no network;
- no Space Replica access;
- no JIT-Node implementation dependency.

Expected module-local diagnostic state after successful start:

```text
PROBE_OK
```

### chat-cli

Planned second public Module. It will validate Module-owned Space data and ordinary Space synchronization between Nodes.

## Private Modules

Private/NDA Modules should live in separate private repositories. Repository access control, not Git branches, is the confidentiality boundary.
