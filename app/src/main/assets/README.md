# Custom Wall Textures for Corridor Crawler

You can place your raw image files directly in this assets folder! The game is configured with an advanced, hardware-accelerated 3D perspective-projection engine that reads these files and applies perspective-skew matrix warping to stretch them smoothly onto the primary corridor walls as you navigate.

## Supported Texture Names

Drop your image files into this directory (`app/src/main/assets/`) with any of the following names and extensions (`.png`, `.jpg`, `.jpeg`, `.webp`):

*   **`wall_lockers.png`** (or `.jpg`, etc.) — Drawn on wall panels when the hallway theme generates locker bays (e.g. steel lockers).
*   **`wall_bulletin.png`** — Special school-themed bulletin boards with announcements and flyers.
*   **`wall_tiles.png`** — Grungy, dirty, or tiled hallway panels.
*   **`wall_cracked.png`** — Peeling or decaying plaster and wall damage.
*   **`wall_water.png`** — Leakages and creeping dark damp stains overlaid on top of regular wall panels.
*   **`wall_graffiti.png`** — Spooky, custom schoolyard graffiti overlaid on top of the walls.

## How it Works under the Hood

1. **Hardware-Accelerated Perspective Projection**:
   During 3D viewport canvas redraws under `RetroViewport.kt`, native canvas transformation matrices are created. 
2. **Matrix Projection via `setPolyToPoly`**:
   The 2D corners of your custom texture boundaries are projected to the 4 skewed perspective corners `(p1, p2, p3, p4)` dynamically calculated for each corridor segment based on depth and camera position:
   ```kotlin
   val srcPts = floatArrayOf(0f, 0f, bmpW, 0f, bmpW, bmpH, 0f, bmpH)
   val dstPts = floatArrayOf(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)
   val matrix = android.graphics.Matrix()
   matrix.setPolyToPoly(srcPts, 0, dstPts, 0, 4)
   ```
3. **Adaptive Depth Shading**:
   The engine automatically applies depth-based lighting to your texture bitmaps so they fade beautifully into the dark corridor shadows as they recede into the distance!
