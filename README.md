# Viwoods Ink SDK Unofficial

An experimental, unofficial Android compatibility layer for low-latency Viwoods e-ink pen input and partial refresh.

## Not an Official SDK

This project is not affiliated with, endorsed by, or supported by Viwoods.

It uses private Viwoods ROM interfaces discovered through device testing and reverse engineering. Viwoods is under zero obligation to keep these interfaces available, stable, compatible, or behaving the same way across firmware updates.

Use this as a best-effort compatibility layer, not as a vendor-supported API.

## Status

Early proof of concept. The fast path has been validated on a Viwoods AiPaper Mini ROM where normal Android touch rendering is visibly slower than the native notes app.

Known current shape:

- The sample app targets SDK 30.
- The library is Java-only for now, but is directly usable from Kotlin.
- Hidden Viwoods APIs are accessed by reflection.
- No Viwoods proprietary classes, APK code, or native libraries are bundled.

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
                // Draw event into inkBitmap and return the local dirty rect.
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

Recommended lifecycle:

```java
// after the view has a measured size and the bitmap exists
controller.startWithResult();

// when the backing bitmap is replaced or resized
controller.refreshBitmap();

// in onPause/onDestroyView
controller.stop();
```

## Proven Fast Path

The working sequence is:

1. Register `ENoteSetting.setWritingInputlistener(...)`.
2. Enable writing input.
3. Configure foreground and background Java bitmaps with the view's screen offset.
4. On stroke start, call `onWritingStart()` and refresh the foreground Java bitmap.
5. Treat native `ACTION_HOVER_MOVE` (`7`) with pressure as in-stroke move data.
6. Draw immediately into the shared bitmap.
7. Send screen-coordinate dirty rects to `renderWriting(...)`.
8. On up/cancel, flush dirty rects and call `onWritingEnd()`.

## Build

```bash
./gradlew :viwoods-ink:assemble :sample:assembleDebug
```

The sample APK will be under `sample/build/outputs/apk/debug/`.
