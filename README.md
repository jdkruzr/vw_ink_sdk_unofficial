# Viwoods Ink SDK Unofficial

An experimental, unofficial Android compatibility layer for low-latency Viwoods e-ink pen input and partial refresh.

## Not an Official SDK

This project is not affiliated with, endorsed by, or supported by Viwoods.

It uses private Viwoods ROM interfaces discovered through device testing and reverse engineering. Viwoods is under zero obligation to keep these interfaces available, stable, compatible, or behaving the same way across firmware updates.

Use this as a best-effort compatibility layer, not as a vendor-supported API.

## Status

The direct callback path was validated on 2026-08-23 with a Viwoods AiPaper Mini running
firmware 3.14.5 (`mp1V9`, Android 13) and WiNote 1.6.5. ForestNote uses it for native-class
latency in an ordinary sideloaded `untrusted_app_30` process.

Known current shape:

- The sample app targets SDK 30.
- The library is Java-only for now, but is directly usable from Kotlin.
- Hidden Viwoods APIs are accessed by reflection.
- Renderer callbacks use the associated view's UI thread by default. Advanced integrations can
  opt into the lower-latency ENote worker thread with `directInputCallbacks(true)`; their renderer
  and listeners must then be thread-safe and must not mutate Views.
- No Viwoods proprietary classes, APK code, or native libraries are bundled.
- The optional native preview registers a screen region with the ROM's MIPI auto-draw service;
  it falls back cleanly when that hidden API is absent.

### Root is not required

The library does not invoke `su`, install a privileged helper, or require a system-app identity.
The validated process ran under Android's ordinary `untrusted_app_30` SELinux domain. Root was
useful while reverse engineering the ROM and installing builds on a device whose ADB/package
shell is restricted, but it is not part of the runtime architecture. A normal APK sideload is
enough on the validated firmware.

These are still private, undocumented ROM APIs. A future Viwoods update can change or remove
them, so applications should keep a normal Android-input fallback.

## Modules

- `viwoods-ink`: Android library with the controller and reflection bridge.
- `sample`: Minimal drawing app using the fast ink path.

## Basic Use

Implement a bitmap provider and renderer, then start the controller when your view is visible:

```java
ViwoodsInkController controller = new ViwoodsInkController(
        view,
        new ViwoodsBitmapProvider() {
            @Override
            public Bitmap getInkBitmap() {
                return inkBitmap;
            }
        },
        new ViwoodsInkRenderer() {
            @Override
            public Rect onInkEvent(ViwoodsInkEvent event) {
                if (event.actionType == ViwoodsInkAction.DOWN) {
                    // Start app stroke state.
                } else if (event.actionType == ViwoodsInkAction.MOVE) {
                    // Draw into inkBitmap.
                } else if (event.isUpOrCancel()) {
                    // Finish app stroke state.
                }
                // Return the changed area in local view coordinates.
                return dirtyRect;
            }
        });

ViwoodsInkStartResult result = controller.startWithResult();
if (!result.started) {
    Log.w("Ink", result.status + ": " + result.detail);
}
```

Kotlin can consume the same API normally:

```kotlin
val controller = ViwoodsInkController(
    view,
    ViwoodsBitmapProvider { inkBitmap },
    ViwoodsInkRenderer { event ->
        drawStrokeEvent(event)
    }
)
```

Advanced integrations can observe stroke boundaries and render failures through `ViwoodsInkConfig`:

```java
ViwoodsInkConfig config = ViwoodsInkConfig.builder()
        .renderBatchSize(2)
        .dirtyRectPaddingPx(12)
        .clipDirtyRectsToView(true)
        .listener(new ViwoodsInkListener() {
            @Override
            public void onStrokeStart(ViwoodsInkEvent event) {
                // Optional: synchronize app stroke state with Viwoods writing mode.
            }

            @Override
            public void onStrokeEnd(ViwoodsInkEvent event) {
                // Optional: flush app-side stroke state.
            }

            @Override
            public void onRenderFailure(ViwoodsInkRenderResult result) {
                Log.w("Ink", result.status + ": " + result.detail);
            }
        })
        .build();
```

For the lowest latency, deliver events directly on ENote's worker thread:

```java
ViwoodsInkConfig config = ViwoodsInkConfig.builder()
        .directInputCallbacks(true)
        .invalidateView(false)
        .renderBatchSize(2)
        .build();
```

In this mode the bitmap provider, renderer, and listener callbacks must be thread-safe and must
not read or mutate Android Views. The controller captures view geometry during UI-thread lifecycle
calls and freezes that origin for each stroke. If the view moves, call `refreshBitmap()` from the
UI thread before accepting more input.

Renderers should return the precise local area they changed. The controller can
then add a configured padding margin, clip to the view bounds, batch dirty rects,
and convert the result to screen coordinates for `renderWriting(...)`.

Recommended lifecycle:

```java
// after the view has a measured size and the bitmap exists
controller.startWithResult();

// when the backing bitmap is replaced or resized
controller.refreshBitmap();

// when the app changes a known bitmap region outside the pen event path
controller.renderNow(localDirtyRect);

// in onPause/onDestroyView
controller.stop();
```

For firmware-latency black pen preview while the app continues to receive normal Android input,
register the drawable local-view region after startup. Tool type `2` is pen (`4` is eraser) and
the width range is in panel pixels:

```java
controller.enableNativePreview(new Rect(0, 0, view.getWidth(), view.getHeight()), 2, 2, 8);
// Before covering the editor, switching to an incompatible tool, or stopping:
controller.disableNativePreview();
```

The ROM tracks regions by calling PID. Always remove the region when the editor is obscured;
`stop()` also removes it defensively.

`ViwoodsInkEvent.actionType` is the stable action API. `action` and `rawAction` are kept for
diagnostics and Android interop; app code should prefer `actionType`, `isDown()`, `isMove()`, and
`isUpOrCancel()`. `rawX`/`rawY` are the absolute ENote coordinates;
`screenOffsetX`/`screenOffsetY` record the frozen origin used to produce local `x`/`y`.

## Proven Fast Path

The working sequence is:

1. Register `ENoteSetting.setWritingInputlistener(...)`.
2. Enable writing input.
3. Configure foreground and background Java bitmaps with the view's screen offset.
4. On stroke start, call `onWritingStart()` and refresh the foreground Java bitmap.
5. Treat native `ACTION_HOVER_MOVE` (`7`) with pressure as in-stroke move data.
6. Draw immediately into the shared bitmap.
7. Pad, clip, batch, and send screen-coordinate dirty rects to `renderWriting(...)`.
8. On up/cancel, flush dirty rects and call `onWritingEnd()`.

ForestNote's integration notes and failure-mode analysis are in
[`docs/research/viwoods-native-ink-2026-08.md`](https://github.com/jdkruzr/ForestNote/blob/main/docs/research/viwoods-native-ink-2026-08.md).

## Build

```bash
./gradlew :viwoods-ink:assemble :sample:assembleDebug
```

The sample APK will be under `sample/build/outputs/apk/debug/`.

The sample includes controls for clearing the bitmap, resetting overlay counters,
cycling render batch size between 1, 2, and 4, and cycling through local renderer
approximations of the Viwoods/Wisky pen types found in WiNote/Wread resources.
