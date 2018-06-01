WorldBorder-Forge is an unofficial and unendorsed port of [Brett Flannigan](https://github.com/Brettflan)'s [WorldBorder](https://github.com/Brettflan/WorldBorder) Bukkit plugin to Forge 1.10.2. This version has been updated and being maintained by abused_master for minecraft versions 1.10.2+. It is a server-only mod that allows the easy management of world sizes. Almost all the features of the Bukkit plugin are available in this version, including but not limited to:

* Per-dimension border control by radius or by corner points
* Round or square borders, with per-dimension overrides
* Knocking back players from borders
* Wrap-around teleportation
* Deny enderpearl, mob spawning and block placements outside borders
* Generation of all chunks within a border with padding, fill rate and auto-save
* Trimming of all chunks and region files outside a border with trim rate

# Requirements

* Minecraft Forge server for 1.10.2, at least [1.10.2-12.18.3.2511](https://files.minecraftforge.net/)

# Mod support

* Integrates optionally with [Dynmap-Forge](https://minecraft.curseforge.com/projects/dynmapforge) for the automatic display of borders

# Installation

1. Download the [latest release JAR](https://minecraft.curseforge.com/projects/worldborder-forge) or clone this repository & build a JAR file
2. Place the JAR file in the `mods/` directory of the server
3. Run/restart the server
4. Open `config/WorldBorder/main.cfg` and modify configuration to desired values
5. Execute `/wb reload` to reload changes

# Differences

This fork is based off version 1.8.4 of the WorldBorder Bukkit plugin. A lot of the codebase has been refactored and reformatted to get it to work best as a Forge mod. As such, it has differences in function and is most likely buggier. Some are intentional, others need further work. These include but are not limited to:

* Use of .cfg files than .yml files, making configuration backwards incompatiable
* [Unreliable handling of teleport events](https://github.com/Gamealition/WorldBorder-Forge/issues/1)
* Unreliable handling of unloaded dimensions
* Uses "DIM##" instead of friendly names for dimensions
* No support for portal redirection
* Incomplete/untested API
* Single-threaded design; no concurrency-safe collections or patterns are used
* Periodic tasks (border check, fill, trim) use tick handlers instead of timers
* No debug mode, in favor of Log4J debug levels
* All WB commands only work for OPs of level 2 or more (vanilla default is 4)

# Building

## Requirements

* [Gradle installation with gradle binary in PATH](http://www.gradle.org/installation). Unlike the source package provided by Forge, this repository does not include a gradle wrapper or distribution.

## Usage
Simply execute `gradle setupCIWorkspace` in the root directory of this repository. Then execute `gradle build`. If subsequent builds cause problems, do `gradle clean`.

# Debugging

WorldBorder-Forge makes use of `DEBUG` and `TRACE` logging levels for debugging. To enable these messages, append this line to the server's JVM arguments:

> `-Dlog4j.configurationFile=log4j.xml`

Then in the root directory of the server, create the file `log4j.xml` with these contents:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration monitorInterval="5">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="[%d{HH:mm:ss} %-4level] %logger{36}: %msg%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="Console"/>
    </Root>
    <Logger name="WorldBorder" level="ALL" additivity="false">
      <AppenderRef ref="Console"/>
    </Logger>
  </Loggers>
</Configuration>
```
