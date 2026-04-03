<h1 align="center">Reunion</h1>
<p align="center">
  <img src="https://img.shields.io/badge/Mom%20get%20the-camera-ff69b4?style=for-the-badge" />
</p>
<p align="center">
  The High-performance Minecraft LCE → Java proxy supporting TU13, TU19, Protocol 47 (1.8.x), & ViaVersion.<br>
  Reunion is the first feature complete proxy implementation for LCE, built specifically for performance & use in production environments.
</p>

## Usage (User Proxy)
If you aren't a network owner & just want to play on a Java server:
1. Run the JAR once to generate `config.yml`.
2. Set `java-host` & `java-port` to the server you want to join.
3. If the server is not 1.8.8, set `via.enabled: true`.
4. Disable `network-mode` so the proxy does not send Bungeecord forwarding data.
5. If the server requires an online mode account, run the proxy & use the console command `login` to link your Microsoft account.

## Usage (Network Owner)
If you are running a server network:
1. Set `network-mode: true` in `config.yml`.
2. Add your backend servers in the `servers` section.
3. Configure `forwarding.mode` (`BUNGEECORD`, `BUNGEEGUARD`, or `VELOCITY`) to match what your backend expects.
4. If your backend servers do **NOT** have ViaVersion installed on them, you can set `via.enabled: true` in the config to handle modern versions.
5. Manage permissions using the SQLITE db in `data/permissions.db`  `/perm` command.

## Anticheat Compatibility
Most anticheats are compatible, but standing on blocks without a proper 1:1 translation (e.g slime blocks) may cause flags.
Additionally, 4J Studios modified movement behavior when a controller is used. This logic is incompatible with most modern Java anticheats such as [**Polar**](https://polar.top) or **Grim**.

## Goals
We want to create complete compatibility between LCE & Java across all platforms, whether through official services (XBL/PSN) or reimplementations (RPCN, Pretendo, NetPlay, etc).

## Note for Crawlers
This repository does **not** contain any Minecraft LCE source code. It is a clean-room reimplementation of the LCE protocol, which is heavily based on Java 1.3.2 & 1.6.1.

## Plugin Messaging & Bungeecord
Full support for Bungeecord/Velocity IP forwarding & plugin messaging channels out of the box. For more details on which channels are supported, visit the [implementation source here](https://github.com/Briiqn/Reunion/tree/master/src/main/java/dev/briiqn/reunion/core/network/server/channel/impl).

## API
You can create plugins using our API. It is currently basic & incomplete, but functionality is being developed. Documentation is a work in progress, but LLM generated Javadocs are available in the source.

Add the API via [JitPack](https://jitpack.io):
```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("com.github.briiqn:reunion:<COMMIT>")
}
```

Replace `<COMMIT>` with the latest commit hash or tag from the releases tab.

## Forks & Backporting
We do **not** support client forks that backport features to LCE (such as adding items/features that were never present), excluding official builds or backports initially developed by 4J Studios. Reunion supports 4J Studios' implementation as-is, not community additions.

## Other Projects
* **[Reunion Skins](https://github.com/Briiqn/reunion-skins):** Spigot plugin that forwards LCE skins to your backend so Java players can see them.
* **[Reunion Limbo](https://github.com/Briiqn/reunion-limbo):** A simple limbo plugin

## Credits
* **ViaVersion**
* **4J Studios**
* **Velocity** (Our API is heavily inspired by theirs)
* **LCEMP** (Initial TCP application layer for allowing connections from LCEMP compatible clients)
* **Lucko** (protobufs for spark and sparkprofiler file format & website)
* **PrismarineJS** (Collision & item remapping data)
