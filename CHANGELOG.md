# Changelog

## 0.2.0 — 2026-08-23

- Add opt-in direct delivery on Viwoods' ENote callback thread for native-class latency.
- Normalize ENote hover-with-pressure events into stable stroke actions.
- Add raw coordinates, applied view origin, and callback timing to ink events for diagnostics.
- Freeze the UI-thread-captured view origin for each direct-input stroke; never query Android View
  geometry from ENote's worker thread.
- Add system-server native-preview region registration and defensive cleanup.
- Validate the direct path from a normal, non-root ForestNote process on AiPaper Mini firmware
  3.14.5 (`mp1V9`) with WiNote 1.6.5.

## 0.1.0

- Initial unofficial controller, reflection bridge, display-only mode, sample app, and lifecycle API.
