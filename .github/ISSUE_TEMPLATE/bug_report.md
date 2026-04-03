---
name: Bug report
about: Create a report to help us improve
title: ''
labels: bug
assignees: Briiqn

---

# LCE Bug Report (Reunion)

**Describe the bug**
A clear and concise description of the issue. Specify if this is a crash, a protocol hang, or a logic error in packet translation.

**Mandatory Reproduction Environment**
*   [ ] **Reunion Build:** I am using a **Debug Build** (compiled without `-O3` optimization) to ensure accurate stack traces.
*   [ ] **Client Source:** I am running **smartcmd/MinecraftConsoles** directly from an **IDE** (e.g., Visual Studio 2022) rather than using a pre-compiled nightly binary.

**Version Information**
*   **Reunion Commit Hash:** `[Insert Hash]`
*   **MinecraftConsoles Commit Hash:** `[Insert Hash from smartcmd/MinecraftConsoles]`
*   **Backend Version:** `[e.g., Paper 1.8.8 / Fabric 1.20.1]`

**To Reproduce**
Steps to reproduce the behavior:
1. Start the Reunion proxy in your terminal/IDE.
2. Launch `Minecraft.Client` from your IDE (Visual Studio/CMake).
3. Connect to the proxy (e.g., `127.0.0.1:25565`).
4. Perform action: `....`
5. Observe the error in either the Proxy console or the IDE's output window.

**Expected behavior**
A clear description of what the LCE client should be doing (e.g., "The crafting UI should populate items without disconnecting the client").


**Screenshots / Media**
Capture any relevant UI bugs or in-game behavior from the `MinecraftConsoles` window.

**Additional context**
Add any other context (e.g., `config.yml` settings, if `via.enabled` is true, or specific network forwarding modes like `BUNGEEGUARD`).

**Technical Logs & Stack Traces**
*Because you are running in an IDE and using a Debug build of Reunion, please provide the full stack trace and any packet logs below:*
