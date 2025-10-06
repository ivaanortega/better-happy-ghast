# Changelog
All notable changes to this project will be documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0+mc1.21.9] - 2025-10-06
### Added
- Parked state for Happy Ghast, toggled with a Blaze Rod (requires harness).
- While parked:
  - Unmounted: the ghast stays still.
  - Mounted: increased flying speed (configurable).
- Visual feedback (glow + particles) when toggling.
- JSON config at `config/better-happy-ghast.json` with `parkedSpeedMultiplier`, `maxSpeed`, and debug logging options.

### Notes
- Requires Fabric Loader ≥ 0.15.0 and Fabric API for 1.21.x.
- Compatible with Minecraft `>=1.21 <1.22`.
