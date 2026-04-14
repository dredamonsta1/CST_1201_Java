# Galaga Java GUI — Containerization Project

## Project Goal
Containerize a Java Swing Galaga arcade game using Docker + Xvfb + x11vnc + noVNC so it can be played in a browser without any local Java installation. Primary purpose is a portfolio piece demonstrating containerization skills.

## Source Files
- `Galaga.java` — entry point
- `GamePanel.java` — main game panel/loop
- `Enemy.java` — enemy logic
- `Explosion.java` — explosion animation

## Build Setup
- Plain `javac` compilation — no Maven or Gradle
- `.class` files are gitignored
- Repo is on GitHub

## Java Version
- OpenJDK 25.0.2 (installed via Homebrew on macOS)
- 64-bit Server VM, mixed mode

## Containerization Stack
```
Java Swing App (Galaga)
      ↓
Xvfb (X Virtual Framebuffer — virtual display inside container)
      ↓
x11vnc (VNC server — captures the virtual display)
      ↓
noVNC (WebSocket bridge — makes VNC accessible in browser)
      ↓
Browser (player just opens a URL, no installs needed)
```

## Container Goals
- Single Dockerfile that compiles and runs the game
- Xvfb creates a virtual display (no physical monitor needed)
- x11vnc serves the display over VNC
- noVNC exposes it as a browser-accessible WebSocket on a defined port
- Player opens browser, plays Galaga — no JDK or VNC client required

## Files To Create
- `Dockerfile` — full build and run instructions
- `start.sh` — startup script to launch Xvfb, x11vnc, noVNC, and the Java app in sequence
- `docker-compose.yml` — optional, for easier local spin-up

## Notes
- Base image should include OpenJDK and be Linux-based (e.g. `openjdk:25` or `eclipse-temurin:21` as fallback if 25 isn't available on Docker Hub)
- Xvfb display is typically set to `:99`
- noVNC default port is `6080` — expose this for browser access
- x11vnc should bind to the Xvfb display and listen on `5900`
- `DISPLAY=:99` environment variable must be set before launching the Java app

## Portfolio Integration (andrewilkinson.com)

### Goal
Embed the running Galaga container as a playable screen on the portfolio site via iframe or noVNC web client.

### Display Concept
- A CRT/monitor-style frame on andrewilkinson.com contains the live Galaga game
- Styled to match the quantum theme — `--qm-cyan` border, scanline overlay, `--qm-surface` background
- Positioned on the left or right open "screen" area of the portfolio layout

### How It Works
- noVNC serves the game on port `6080` from the hosted container
- Portfolio embeds it via `<iframe src="https://your-container-host:6080/vnc.html" />`
- Player clicks into the iframe and plays directly in the browser

### Hosting Candidates for the Container
- **Fly.io** — preferred, stays alive, straightforward Docker deploy
- **Render** — free tier but spins down on inactivity
- **Railway** — simple Docker deploys, small free allowance

### Notes
- Container host URL needs to be set as an env variable in the portfolio repo so it's easy to swap
- noVNC has its own UI chrome — may need `?autoconnect=1&resize=scale&show_dot=1` query params to strip it down for embedded use
- Scanline CSS overlay on the iframe wrapper will tie it into the quantum aesthetic