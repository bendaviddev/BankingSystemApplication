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

### Changed
- Mark web fonts as binary and collapse frontend lockfiles in diffs (`.gitattributes`).

## [2.0.0]

### Added
- Fintech dashboard frontend: landing page, transfer flow, analytics, admin, dark mode.
- Backend v2: Flyway migrations, BigDecimal money handling, DB-backed sessions, alerts, analytics, admin, and seed data.

### Changed
- Redact recipient leg from external transfer responses.
