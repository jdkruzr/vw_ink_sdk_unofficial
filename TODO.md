# TODO

## API Hardening

- Define a stable normalized action model instead of exposing Android/Viwoods raw action values as the primary API.
- Document the renderer thread contract. Current callbacks are delivered on the view/UI thread.
- Decide whether to support an advanced direct-callback-thread mode for lower latency.
- Add explicit stroke lifecycle hooks for advanced integrations that need to observe start/end separately from draw events.
- Add richer runtime error reporting for render failures after startup.
- Define how apps should handle bitmap replacement, page changes, view movement, and orientation changes.
- Add Kotlin convenience wrappers once the Java API stabilizes.

## Rendering Behavior

- Validate dirty-rect batching defaults against WiNote behavior on-device.
- Decide whether dirty rect padding belongs in the library config, the app renderer, or both.
- Add sample renderers for full-segment drawing and optional smoothing.
- Document that dirty rects returned by renderers must be in local view coordinates.
- Add guardrails for offscreen/empty dirty rects and unusual native event sequences.

## Device Validation

- Test the sample APK on the current AiPaper Mini ROM.
- Record device model, firmware version, target SDK, and result in a compatibility table.
- Test target SDK 31 and newer again with the extracted library.
- Check behavior after sleep/wake, app switch, rotation/config changes, and repeated start/stop cycles.
- Verify no lingering ENote listener or writing state remains after `stop()`/`detach()`.

## ForestNote Integration

- Add the library to ForestNote as a local module or included build.
- Implement a Viwoods fast-ink backend behind a feature flag/fallback path.
- Map ForestNote stroke tools and pressure handling to `ViwoodsInkRenderer`.
- Ensure page/canvas bitmap lifecycle calls `refreshBitmap()` at the right times.
- Keep the existing ForestNote Viwoods path as fallback until the new path is validated.
- Test latency, fast strokes, eraser/tool switching, page changes, and app pause/resume on-device.

## Sample App

- Add a small debug/status overlay with availability, start result, event count, render count, and last failure.
- Add clear/reset controls.
- Add a toggle for render batch size.
- Add a simple APK install/copy workflow for the Viwoods device.

## Publishing

- Choose Maven coordinates and package name policy.
- Add a license.
- Add CHANGELOG.
- Add contribution guidelines.
- Add compatibility/support policy.
- Decide when to make the GitHub repo public.
- Prepare an initial release tag only after ForestNote integration validates the library API.
