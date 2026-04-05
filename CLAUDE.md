# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LMRBCompat is a Minecraft mod that provides compatibility between the LittleMaidRebirth mod and various weapon/tool mods. The project uses the Architectury framework to support both Fabric and Forge mod loaders on Minecraft 1.20.1.

## Architecture

This is a multi-platform mod using Architectury's structure:

- **common/**: Platform-independent code and shared compatibility logic
- **fabric/**: Fabric-specific implementations and integrations
- **forge/**: Forge-specific implementations and integrations

### Key Components

1. **Compatibility System**: Uses `loadCompat()` with direct class references and `try-catch (LinkageError)` to safely enable compatibility modules when specific mods are detected
2. **Mode System**: Abstract mode classes (like AbstractShooterMode) that define behavior patterns for different weapon types
3. **Configuration**: Per-mod configuration files using Cloth Config for settings management
4. **Mixin Integration**: Uses Mixin to modify existing mod behavior for compatibility

### Supported Mods

- ActionArms (Fabric/Forge)
- ClassicGuns (Forge only)
- FN5728 (Forge only)
- GVCLib (Forge only)
- SlashBlade variants (Forge only)
- HandmadeGuns2 (Forge only, GVCLib が前提 Mod のため GVCLib 互換で対応)

## Build Commands

### Development Environment Setup
```bash
# Build all modules
./gradlew build

# Build specific platform
./gradlew :fabric:build
./gradlew :forge:build

# Run development client
./gradlew :fabric:runClient
./gradlew :forge:runClient

# Run development server
./gradlew :fabric:runServer
./gradlew :forge:runServer
```

### Testing
```bash
# Run in development environment with test mods
# Fabric: mods in fabric/mods/ and fabric/mods_runtime/
# Forge: mods in forge/mods/ and forge/compile_only_mods/
```

## Development Notes

### Adding New Compatibility Modules

1. Create compat class in `common/src/main/java/net/sistr/lmrbcompat/[modname]/`
2. Add mode implementations extending AbstractShooterMode or similar
3. Add configuration class if needed
4. Register in `LMRBCompat.init()` (common) or `LMRBCompatForge.onCommonSetup()` (Forge) using `LMRBCompat.loadCompat("modid", ModCompat::new)`
5. For Forge-specific features, add Compat class under `forge/` and register from `LMRBCompatForge`

### Configuration Structure

Each mod compatibility has its own config file in JSON format:
- Enable/disable compatibility
- Per-mode settings
- Behavior customization options

### Mixin Usage

The mod uses Mixins to modify behavior of:
- LittleMaidRebirth entities
- Weapon mod rendering systems
- Attack/behavior management systems

Files are organized in `mixin/[platform]/[modname]/` directories.

### Package Structure

```
net.sistr.lmrbcompat/
├── [modname]/          # Mod-specific compatibility
│   ├── [ModName]Compat.java
│   ├── [ModName]Config.java
│   └── mode/           # Behavior modes
├── compat/             # Common compatibility utilities
├── client/             # Client-side features
├── mode/               # Abstract mode definitions
└── reflection/         # Reflection utilities
```

## Important File Locations

- Main mod class: `common/src/main/java/net/sistr/lmrbcompat/LMRBCompat.java`
- Platform entry points: `fabric/src/main/java/net/sistr/lmrbcompat/fabric/LMRBCompatFabric.java`
- Mod versions: `gradle.properties`
- Mod metadata: `fabric/src/main/resources/fabric.mod.json`, `forge/src/main/resources/META-INF/mods.toml`

## Cross-Environment Workflow

- WSL2 から Windows リポジトリへローカルremote経由で転送可能
- `git remote add local /mnt/v/Develop/Minecraft/LMRBCompat`
- Windows側でチェックアウト中のブランチにはpush不可。別ブランチ名にpush: `git push local 1.20.1:wsl/{branch-name}`

## Environment Notes

- WSL 環境では `runClient` が失敗する（アセットダウンロード不可）。Windows 側のリポジトリで実行すること
- ブランチ切り替え後に `chmod +x gradlew` が必要な場合がある
- Forge ビルドで `minecraft-merged-srg.jar` の `FileAlreadyExistsException` が出たらキャッシュを削除する