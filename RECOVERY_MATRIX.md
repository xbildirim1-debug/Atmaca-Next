# Recovery Engine 2.0 Matrix

- Accessibility service reconnect: relaunch X -> verify target @handle -> reopen required list.
- X process/app restart: discard stale nodes and pending clicks; relaunch and verify from scratch.
- Wrong/unknown screen: never follow/unfollow; recover to known X surface.
- Account mismatch: stop action path and re-enter account verification/switch flow.
- Stalled list/navigation: bounded recovery; after 3 failed attempts PAUSED.
- Unknown dialog: no positive/permission guess; safe back/known-screen recovery only.
- Recovery never increments the 35 counter. Only normal ActionVerifier may increment it.
