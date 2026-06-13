# Security Policy

SkyBlock Lens is a client-side quality-of-life mod. It must not collect account
tokens, session data, API keys, personal data, or secrets.

## Rules

- No telemetry, ads, trackers, token collection, or hidden network requests.
- No remote code execution.
- No downloading executable files such as jar, dll, exe, bat, or ps1.
- Network features must be opt-in, documented, rate-limited, and disableable.
- Missing local data must degrade gracefully instead of crashing the game.
- Suspicious feature requests should be rejected before implementation.

## Reporting

Open a GitHub issue or private maintainer contact for security problems. Do not
publish exploit details until a fix is available.
