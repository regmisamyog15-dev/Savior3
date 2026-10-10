# Savior — Privacy-First Live Screenshot, Screen Recorder & Media Editor Suite

A privacy-first architectural overhaul of Savior that eliminates all persistent background capture sessions, introduces a high-performance screen recording suite with confirmed overlay suppression, and provides lightweight on-device media editors for screenshots and video clips.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Privacy-First Core Principle:**
> * **Idle State:** ZERO `MediaProjection`, ZERO `VirtualDisplay`, ZERO background frame reader. Savior is completely passive.
> * **Screenshot:** User requests capture → Android permission requested and approved → Savior overlays hidden & confirmed removed → single frame captured → `MediaProjection` destroyed immediately → Savior restored → floating preview pill shown.
> * **Recording:** `MediaProjection` exists ONLY while actively recording.
> * **Overlay Suppression:** Savior guarantees that its own overlays are removed before capture begins and are not intentionally included in the capture.

* **Revised Screenshot Permission Sequence:** The permission flow strictly requests and receives user consent *before* hiding overlays, preventing awkward UI states where Savior disappears while the system dialog is pending.
* **Realistic Clean Capture Wording:** Acknowledging OEM differences, Savior guarantees that its own overlays (Orb, radial menu, panels) are fully detached from `WindowManager` before capture initiates and are not intentionally included in the capture.
* **Reliable Pause / Resume:** Utilizes native API 24+ pause with seamless segmented file recording / muxer concatenation as the fallback to ensure audio/video synchronization without fake timers.
* **Hardware-Grounded 60 FPS:** Presets explicitly state "1080p / 60 FPS where supported", dynamically querying encoder profile capabilities (`MediaCodecList` / `CamcorderProfile`).
* **Architectural Separation:** 
  1. `ScreenCaptureManager`: One-shot ephemeral screenshot capture.
  2. `ScreenRecorderService`: Explicit user-triggered recording session.
  3. `OverlayStateManager`: Controls overlay visibility and guarantees detachment before recording surfaces activate.
* **Capture Privacy Status in Settings:** A dedicated live privacy indicator in Settings showing the real-time state:
  * ● **Not active:** "Savior isn't accessing your screen. When you take a screenshot or start recording, Android will ask for screen-capture permission."
  * 🔴 **Screen capture active:** "Recording in progress."
  * ✓ **Screen capture ended:** "Session released and destroyed."

---

## 1. Overview & Core Concept

* **What It Does:** Savior eliminates continuous background recording and replaces it with an on-demand, privacy-first capture engine. Users can take clean, high-resolution screenshots with an instant preview pill and markup editor, or record high-definition video (1080p 30/60 FPS where supported) with flexible audio options (None, Mic, Device, Both) and a dedicated video trimmer.
* **Target Audience & Context:** Users creating screen walkthroughs, reporting bugs, saving media clips, or capturing gaming highlights who expect zero background surveillance, no persistent screen-sharing indicators, and pristine captures free of intrusive UI buttons.
* **Core Value:** **"Capture only when you ask, record only what matters."** Complete transparency, zero background battery/privacy drain, and clean captures ready to edit and share in seconds.

---

## 2. User Experience & Visual Design

### Key User Flows

#### A. Revised One-Shot Live Screenshot Flow
1. **Trigger:** User taps "Live Screenshot" from the Savior Floating Orb radial menu, Home screen card, or Quick Settings tile.
2. **Authorization (If Needed):** System prompts standard Android `MediaProjection` permission dialog while Savior UI remains in its normal state.
3. **User Approves Permission:** Once authorization is confirmed by the system:
4. **Overlay Suppression:** `OverlayStateManager` immediately instructs `WindowManager` to remove all Savior floating overlays (Orb, radial dial, volume HUD). Savior verifies overlays are confirmed detached.
5. **Frame Acquisition:** `ScreenCaptureManager` initializes an ephemeral `VirtualDisplay` and `ImageReader`, acquires the single pristine display frame, and immediately stops and releases `MediaProjection`.
6. **Overlay Restoration & Preview Pill:** `OverlayStateManager` restores the Savior Orb, and `FloatingScreenshotPill` displays at the bottom of the screen with a thumbnail, `[Edit]`, `[Share]`, and auto-dismiss after 6 seconds.
7. **Editing (Optional):** Tapping `[Edit]` opens `ScreenshotEditorActivity` for cropping, freeform drawing, highlighters, shape markup, and direct MediaStore export.

```
┌────────────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐
│ User Taps Live SS      │  ───► │ Android Permission UI  │  ───► │ User Grants Consent    │
│ (Savior UI Visible)    │       │ (Normal App State)     │       │                        │
└────────────────────────┘       └────────────────────────┘       └───────────┬────────────┘
                                                                              │
                                                                              ▼
┌────────────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐
│ Capture 1 Clean Frame  │  ◄─── │ Wait Detachment Sync   │  ◄─── │ Hide Savior Overlays   │
│ & STOP Projection Now! │       │ (Confirmed Removed)    │       │ (OverlayStateManager)  │
└───────────┬────────────┘       └────────────────────────┘       └────────────────────────┘
            │
            ▼
┌────────────────────────┐       ┌────────────────────────┐       ┌────────────────────────┐
│ Restore Savior Orb     │  ───► │ Floating Preview Pill  │  ───► │ Screenshot Editor      │
│ (Normal Idle State)    │       │ [Thumb] [Edit] [Share] │       │ (Doodle / Crop / Save) │
└────────────────────────┘       └────────────────────────┘       └────────────────────────┘
```

#### B. Screen Recorder Flow
1. **Configuration Screen:** User opens "Screen Recorder" from the Orb or Home card:
   * **Video Quality:** Standard (1080p, 30 FPS), High (1080p, 60 FPS where supported), Maximum (Native device resolution & max supported FPS).
   * **Audio Source:** No Audio, Device Audio, Microphone, Device Audio + Microphone (with runtime permission validation).
   * **Clean Recording:** "Hide Savior controls from recording" (Default: ON).
   * **Recording Controls:** Notification only (Default) vs. Floating controls + Notification.
2. **Start Sequence:**
   * User taps "Start Recording".
   * MediaProjection permission dialog appears and is accepted.
   * `OverlayStateManager` hides all Savior overlays and verifies detachment.
   * MediaRecorder/MediaCodec pipeline activates and records.
3. **During Recording:**
   * Persistent Android notification shows live timer, `[Pause]`, and `[Stop]`.
   * Floating Orb is completely absent from the screen.
   * Optional floating pill is visible only if explicitly enabled in settings.
4. **Stop & Completion:**
   * User taps `[Stop]` from notification.
   * Recording finalize completes, projection is terminated, Savior Orb is restored.
   * `VideoEditorActivity` launches automatically with the new clip.

---

### Visual Identity & Theme

* **Atmosphere:** Deep graphite (`#121417` / `#181B1E`) with warm brass (`#D2BC94`) and pearl (`#EEF0F2`) accents.
* **Status Accents:**
  * Sage Green (`#98C379`) for verified idle/clean states.
  * Rose/Crimson (`#E06C75`) for active recording indicators and mute toggles.
  * Dim Slate (`#8E949C`) for secondary metadata and technical specs.
* **Component Styling:** Pill-shaped floating previews, generous 48dp+ interactive touch targets, and non-blocking translucent panels with 1.2dp border accents.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Elimination of Persistent Buffer (Live 1 Removal)
* **Approach:** Delete continuous rolling frame grabbers and persistent foreground projection services.
* **Why:** Eliminates the permanent Android screen-sharing indicator, stops background battery drain, and honors user privacy.
* **Trade-Off:** Users capture on explicit request rather than looking back into past seconds.

### Decision 2: Realistic Clean Capture Contract
* **Approach:** Savior guarantees that its own overlays are removed before capture begins and are not intentionally included in the capture.
* **Why:** Avoids overpromising across thousands of OEM ROM variants while implementing rock-solid `WindowManager` synchronization.

### Decision 3: Segmented Pause vs. Encoder Pause
* **Approach:** Use standard `MediaRecorder.pause()` on API 24+ with try/catch; if hardware does not support seamless resume, record multi-segment MP4s and concatenate via `MediaMuxer` during finalization.
* **Why:** Prevents video drift, desynchronized audio tracks, and crash errors like `MPEG4Writer: Stop() called but track is not started`.

### Decision 4: Capture Privacy Status in Settings
* **Approach:** A live status dashboard in Savior Settings showing:
  * ● **Not active:** Savior is idle with zero capture resources active.
  * 🔴 **Screen capture active:** Recording session currently running.
  * ✓ **Screen capture ended:** Session cleanly torn down.
* **Why:** Gives users complete peace of mind that Savior is not watching or recording their screen in the background.

---

## 4. Technical Architecture & Component Separation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                               SAVIOR CORE                                   │
├──────────────────────────────┬──────────────────────────────────────────────┤
│    ScreenCaptureManager      │            ScreenRecorderService             │
│  - Ephemeral 1-shot capture  │  - Active ONLY during explicit recording     │
│  - Triggered AFTER consent   │  - 1080p 30 / 60 FPS (where supported)       │
│  - Instant projection stop   │  - Segmented/Native Pause handling           │
│  - Emits clean bitmap        │  - Notification-first controls               │
└──────────────┬───────────────┴──────────────────────┬───────────────────────┘
               │                                      │
               ▼                                      ▼
┌──────────────────────────────┐       ┌──────────────────────────────────────┐
│     Floating Preview Pill    │       │         Savior Video Editor          │
│   & Screenshot Editor        │       │  - Precise Trim (start/end sliders)  │
│  - Doodle Pen & Highlighter  │  - Lossless MediaExtractor/Muxer trim│
│  - Crop & Orientation tools  │  - Gallery Export & System Share     │
│  - Gallery & Share intent    │  - Delete & Manage recordings        │
└──────────────┬───────────────┘       └──────────────────────┬───────────────┘
               │                                      │
               └──────────────────┬───────────────────┘
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            OverlayStateManager                              │
│  - Centralized visibility coordinator for all Savior window overlays        │
│  - Enforces: HIDE OVERLAYS -> AWAIT SYNC -> CAPTURE -> RESTORE              │
│  - Feeds live state into Settings Capture Privacy Status                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Roles

| Component | Responsibility | Privacy & Lifecycle |
| :--- | :--- | :--- |
| `ScreenCaptureManager` | One-shot screenshot engine using temporary `MediaProjection`. | Idle = 0 resources. Starts projection only *after* user consent, extracts 1 frame, and immediately releases projection. |
| `ScreenRecorderService` | Explicit foreground recording service. | Exists solely during recording. Manages video/audio encoders, notification timer, and finalization. |
| `OverlayStateManager` | Coordinates visibility of Floating Orb, radial menu, and volume panel. | Explicitly detaches overlays before capture starts and restores them once capture finishes. |
| `FloatingScreenshotPill` | Draggable preview pill with thumbnail, Edit, Share, and auto-dismiss. | Appears *after* capture is completed and projection is already dead. |
| `ScreenshotEditorActivity` | Vector drawing and crop canvas. | Full-featured crop, pen, highlighter, undo/clear, save to Gallery, and share. |
| `VideoEditorActivity` | Lossless video trimmer and player. | Range slider for start/end trim, fast lossless MP4 remuxing, video playback, and share. |
| `CapturePrivacyStatusView` | Settings dashboard showing screen capture state. | Shows Not Active / Active / Ended in real time to reassure users. |

---

## 5. Verification Plan

* **Permission Flow Order Test:** Verify that tapping Live Screenshot shows the system permission prompt *while Savior is still fully intact*, and overlays disappear *only after* consent is tapped.
* **Idle Resource Audit:** Verify via system dumpsys/inspection that while Savior runs in the background, zero `VirtualDisplay`, zero `ImageReader`, and zero `MediaProjection` instances exist.
* **Overlay Suppression Verification:** Verify that screenshots and video recordings do not contain any Savior UI pixels.
* **Pause / Resume Integrity:** Test pause and resume during screen recording; verify no audio-video sync drift occurs and MP4 files are cleanly playable.
* **Privacy Status in Settings:** Verify Settings reflects "Not active" when idle, "Active" during recording, and "Ended" upon completion.
* **Build & Test Suite:** Run `compile_applet` and `:app:testDebugUnitTest` to guarantee zero compile errors or regressions.
