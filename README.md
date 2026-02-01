# BizFlow (team setup)

## Java versions (important)

- **Project compile target:** Java **17** (all `pom.xml` use `<java.version>17</java.version>`).
- **VS Code / IDE toolchain:** recommended Java **21**.
  - Reason: some Java extensions + Lombok annotation processing can break on newer JDKs (e.g. 25), causing many false errors in the Problems panel.
  - This repo includes a local JDK at `.jdk/jdk-21.0.8` and VS Code settings point to it.

This means:
- When you run with Docker, the container’s Java is used (no need to install JDK 21).
- When you build locally, any JDK >= 17 works, but **use 21 in VS Code** for stable tooling.

## VS Code recommended steps (if Problems panel is noisy)

1. Ensure the repo-local JDK exists: `.jdk/jdk-21.0.8`.
2. In VS Code:
   - `Developer: Reload Window`
   - (optional) `Maven: Reload Projects`
   - (optional) `Java: Clean Java Language Server Workspace`

## Build

From repo root:

- `mvn -DskipTests package`

## Run (Docker)

- `docker compose up --build`

