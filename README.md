# lamadb-android

Android client for [LamaDB](https://github.com/aliforfaen/LamaDB) / Life-OS.

## Purpose

A mobile window into LamaDB: notifications, quick capture, dashboard widgets, and life-status summaries on Android.

## Scope

- Tailnet/SSO-aware authentication
- Read-only and quick-write access to LamaDB API
- Push notifications via ntfy / Firebase
- Home screen widgets for ticker, health snapshot, kanban tasks
- Offline caching of critical data

## Status

Skeleton / planning phase. Initial spec written: `docs/spec.md`.

## Development

No Android Studio is required. The project is designed to be built, tested, and deployed from the command line on a headless Linux host (e.g., `norheim`), then installed to your phone via ADB over WiFi through Tailscale.

See:
- [Development environment guide](docs/development.md)
- [Setup script](scripts/setup-dev-env.sh)
- [Agent definitions](docs/agents.md)
- [Desktop setup checklist](docs/desktop-setup.md)
- [Backlog](docs/backlog.md)

## Links

- [LamaDB](https://github.com/aliforfaen/LamaDB)
- [LamaDB Hermes plugin](https://github.com/aliforfaen/lamadb-hermes)
- [Spec sheet](docs/spec.md)
- [Agent definitions](docs/agents.md)