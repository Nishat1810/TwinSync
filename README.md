# PhoneTwinSync — Chat 1 Final

Selective-sync Android project intended to be uploaded to a GitHub repository and built with GitHub Actions.

## Current workflow
- Tapping a file only selects it; it NEVER starts syncing.
- Selected files are held in temporary in-app selection state.
- A visible `SYNC REVIEW` / sync action remains available while browsing.
- Single-file workflow: select one file -> Sync Selected -> approve -> transfer engine.
- Multi-file workflow: select several -> Sync Selected -> approve -> transfer engine.
- Sync approval and deletion approval are separate.
- No silent deletion.

## UI
- Photo previews
- Video thumbnails
- List view
- Grid view
- Tinder-style swipe review
- Photos / Videos / Both filters
- Individual file picker
- Clear status messages for selection, sync review, and deletion review

## Performance direction
The planned transfer engine is direct LAN phone-to-phone, dependency-light, incremental, resumable, and designed to avoid unnecessary CPU/RAM/battery use.

## Important
This package is the UI/selection foundation. The actual authenticated/encrypted LAN transfer engine, manifest comparison, resumable transfer implementation, conflict handling, and per-file deletion engine are the next development stage. The current buttons intentionally do not pretend that files have been transferred.
