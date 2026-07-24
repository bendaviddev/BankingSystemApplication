# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Repository hygiene files: `.editorconfig`, `.gitattributes`, `CONTRIBUTING.md`.
- Ignore rules for editor backup and swap files.
- Code style section in `CONTRIBUTING.md`, linked from the README.
- Ignore rules for Windows OS and temporary files.
- Documented `SERVER_PORT` and `SPRING_PROFILES_ACTIVE` in `.env.example`.
- EditorConfig rule for 2-space indentation in shell scripts.
- Pin the frontend Node version to 20+ via `engines` and `.nvmrc`.
- Standalone `typecheck` npm script for running `tsc --noEmit` without a full build.

### Changed
- Mark web fonts as binary and collapse frontend lockfiles in diffs (`.gitattributes`).
- Trim the Docker build context: exclude editor, temp, and docs files (`.dockerignore`).

## [2.0.0]

### Added
- Fintech dashboard frontend: landing page, transfer flow, analytics, admin, dark mode.
- Backend v2: Flyway migrations, BigDecimal money handling, DB-backed sessions, alerts, analytics, admin, and seed data.

### Changed
- Redact recipient leg from external transfer responses.
