# NotchNet Mod

An AI-powered Minecraft knowledge companion mod. Connects to the [NotchNet Backend](https://github.com/aaravchour/NotchNet) to answer questions about Minecraft and modded content using Retrieval-Augmented Generation (RAG).

## Features

- 🤖 **AI Q&A** — Ask any question about Minecraft or installed mods and get context-aware answers
- 🌐 **Cloud or Local** — Connect to a remote NotchNet server or run the backend locally
- 📚 **Wiki-Aware** — Automatically learns from fetched wikis (Vanilla, RLCraft, FTB, etc.)
- 🎮 **In-Game HUD** — Chat-like interface directly in Minecraft
- 📦 **Multi-Version** — Supports Forge, Fabric, and NeoForge across Minecraft versions 1.7.10 to 1.21.1

## Supported Versions

| Minecraft | Forge | Fabric | NeoForge |
|-----------|-------|--------|----------|
| 1.21.1 | — | — | ✅ |
| 1.20.1 | ✅ | ✅ | — |
| 1.16.5 | ✅ | ✅ | — |
| 1.12.2 | ✅ | — | — |
| 1.7.10 | ✅ | — | — |

## Quick Start

### 1. Install the NotchNet Backend

The mod needs a running NotchNet backend to answer questions:

**Option A: Local (same machine)**
```bash
git clone https://github.com/aaravchour/NotchNet.git
cd NotchNet
./start_local.sh   # or start_local.bat on Windows
```
The backend starts at `http://localhost:8000`.

**Option B: Remote / Cloud**
If you have a NotchNet server running elsewhere (e.g., a VPS or friend's machine), note its IP address and port.

### 2. Configure the Mod

After installing the mod in your Minecraft client:

1. Open your Minecraft config folder:
   - **Windows**: `%appdata%/.minecraft/config/`
   - **Mac**: `~/Library/Application Support/minecraft/config/`
   - **Linux**: `~/.minecraft/config/`

2. Find `notchnet-client.toml` (created on first launch).

3. Set the backend URL:
   ```toml
   [general]
   # NotchNet Backend API URL
   # Default: http://localhost:8000 (local)
   # Remote example: http://192.168.1.100:8000
   apiUrl = "http://localhost:8000"
   ```

4. Restart Minecraft.

### 3. Use In-Game

- Press the **NotchNet key** (default: `N`) to open the chat interface.
- Type your question and press Enter.
- The answer appears in the chat window.

## Cloud Inference

NotchNet supports offloading AI inference to a remote server. This is useful for:

- **Low-end machines** — Run the heavy AI backend on a powerful server while keeping Minecraft lightweight
- **Shared backends** — One NotchNet instance serves multiple players
- **24/7 availability** — Backend stays online even when you close Minecraft

### Setting Up Cloud Mode

1. Ensure your NotchNet backend is publicly accessible (or on your local network).
2. Note the backend's IP address and port (default: `8000`).
3. In the mod config (`notchnet-client.toml`), set:
   ```toml
   apiUrl = "http://YOUR_SERVER_IP:8000"
   ```
4. The mod will now send questions to the remote server instead of `localhost`.

### Backend Requirements for Cloud

- Python 3.10+
- Ollama installed and running (or use Cloud Mode with a remote Ollama instance)
- The backend's `config.yaml` should have `hermes.enabled: true` and point to your Ollama instance.

## Building from Source

```bash
# Clone the repo
git clone https://github.com/aaravchour/notchnet-mod.git
cd notchnet-mod

# Build a specific version
./gradlew :notchnet-1.20.1-forge:build
./gradlew :notchnet-1.20.1-fabric:build
./gradlew :notchnet-1.21.1-neoforge:build

# Find the built JAR in the subproject's build/libs/ directory
ls notchnet-1.20.1-forge/build/libs/
```

## Troubleshooting

| Issue | Fix |
|-------|-----|
| "Cannot connect to backend" | Check that the NotchNet backend is running and `apiUrl` is correct |
| "Connection timed out" | Increase timeout in `CoreConfig.java` or check firewall rules |
| "No answer from server" | Ensure Ollama is running on the backend machine |
| Mod crashes on startup | Check that you're using the correct mod version for your Minecraft version |

## Related Projects

- [NotchNet Backend](https://github.com/aaravchour/NotchNet) — The AI backend that powers this mod
- [notchnet-mod](https://github.com/aaravchour/notchnet-mod) — This mod (client-side)

## License

MIT License. See [LICENSE](LICENSE) for details.
