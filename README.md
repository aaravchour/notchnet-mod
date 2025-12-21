# NotchNet - Minecraft Mod

**NotchNet** brings the power of AI directly into your Minecraft chat. Ask questions, get help with mechanics, or discover details about the mods you're playing with, all without leaving the game.

## 🚀 Features
- **In-Game AI Chat**: Ask questions using `/notchnet <question>`.
- **Mod Discovery**: Automatically detect installed mods and share them with the backend for better context-aware answers.
- **Easy Configuration**: Update your API URL and settings directly from the game.
- **Privacy First**: Fully open-source and customizable.

## 🛠 Commands
- `/notchnet <question>` - Ask the AI a question.
- `/notchnet help` - Show the help menu.
- `/notchnet status` - Check the connection to your NotchNet backend.
- `/notchnet config` - View your current configuration.
- `/notchnet config apiUrl <url>` - Change the backend API URL.
- `/notchnet config autoScanMods <1|0>` - Toggle automatic mod scanning on startup.

## 📦 Installation
1.  **Build the mod**:
    ```bash
    ./gradlew build
    ```
2.  **Add to Minecraft**:
    Copy the generated `.jar` file from `build/libs/` to your Minecraft `mods` folder.
3.  **Requirements**:
    - Fabric Loader (>=0.17.3)
    - Minecraft 1.21.10 / 1.21.11
    - NotchNet Backend running locally or on a server.

## ⚖️ License
This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
