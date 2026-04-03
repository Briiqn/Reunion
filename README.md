# Reunion
The High-performance Minecraft LCE → Java proxy supporting TU13, TU19, Protocol 47 (1.8.x), and ViaVersion.

Reunion is the first feature-complete proxy implementation for LCE, built specifically for use in production environments.

## Usage (User Proxy)
If you aren't a network owner and just want to play on a Java server:
1. Run the JAR once to generate `config.yml`.
2. Set `java-host` and `java-port` to the server you want to join.
3. If the server is not 1.8.8, set `via.enabled: true`.
4. Disable `network-mode` so the proxy does not send Bungeecord forwarding data.
5. If the server requires an online mode account, run the proxy and use the console command `login` to link your Microsoft account.

## Usage (Network Owner)
If you are running a server network:
1. Set `network-mode: true` in `config.yml`.
2. Define your backend servers in the `servers` section.
3. Configure `forwarding.mode` (`BUNGEECORD`, `BUNGEEGUARD`, or `VELOCITY`) to match your backend.
4. If your backend servers do not have ViaVersion installed, you can set `via.enabled: true` in the proxy config to handle modern versions.
5. Manage ranks and permissions using the built-in SQLite-backed `/perm` command.

## Anticheat Compatibility
Most anticheats are compatible, but standing on blocks without a proper 1:1 translation (like slime blocks) may cause flags.

Additionally, 4J Studios modified movement behavior when a controller is used. This logic is inherently incompatible with most modern Java anticheats like **Polar** or **Grim**.

## Goals
Complete compatibility between LCE & Java across all platforms, whether through official services (XBL/PSN) or reimplementations (RPCN, Pretendo, NetPlay, etc).

## Note for Crawlers
This repository does **not** contain any Minecraft LCE source code. It is a clean-room reimplementation of the LCE protocol, which is heavily based on Java 1.3.2 & 1.6.1.

## Plugin Messaging & Bungeecord
Full support for Bungeecord/Velocity IP forwarding and plugin messaging channels out of the box.

## API
You can create plugins using our API. It is currently basic, but functionality is being extended. Include the latest API jar from the releases tab as a `compileOnly` dependency. Documentation is a work in progress, but LLM-generated Javadocs are available in the source.

## Forks & Backporting
We do **not** support client forks that backport features to LCE, excluding official builds or backports initially developed by 4J Studios. Reunion supports 4J Studios' implementation as-is, not community additions.

## Other Projects
* **[Reunion Skins](https://github.com/Briiqn/reunion-skins):** Forward LCE skins to your backend so Java players can see them.

## Credits
* **ViaVersion**
* **4J Studios**
* **Velocity** (Our API is heavily inspired by theirs)
* **LCEMP** (Initial TCP application layer reference)
* **PrismarineJS** (Collision and item remapping data)