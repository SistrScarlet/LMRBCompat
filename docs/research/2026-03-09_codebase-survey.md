# LMRBCompat コードベース調査レポート

**調査日:** 2026-03-09
**対象バージョン:** 1.2.1 (1.20.1 ブランチ)
**Java ソースファイル数:** 33

## 概要

LMRBCompat は LittleMaidReBirth (LMRB) と各種武器/ツール Mod の互換性を提供する Minecraft Mod。
Architectury フレームワークで Fabric/Forge 両対応。

## アーキテクチャ

### モジュール構成

| モジュール | 役割 |
|-----------|------|
| `common/` | プラットフォーム共通コード（ActionArms 互換、フレームワーク基盤） |
| `fabric/` | Fabric エントリポイント、ModMenu 統合 |
| `forge/` | Forge エントリポイント、ClassicGuns/FN5728/GVCLib/SlashBlade 互換 |

### クラス継承構造

```
AbstractArcherMode (LMRB API)
  └── AbstractShooterMode (common)
      ├── ShooterMode (common/actionarms)
      ├── ShooterMode (forge/classicguns)
      ├── ShooterMode (forge/fn5728)
      └── ShooterMode (forge/gvclib)

AbstractFencerMode (LMRB API)
  └── SlashBladeMode (forge/slashblade)

AbstractCompat<T extends ConfigData>
  ├── ActionArmsCompat (common)
  ├── ClassicGunsCompat (forge)
  ├── FN5728Compat (forge)
  ├── GVCLibCompat (forge)
  └── SlashBladeCompat (forge)
```

### パッケージ構成

```
net.sistr.lmrbcompat/
├── LMRBCompat.java              # メインクラス
├── actionarms/                   # ActionArms 互換（common）
│   ├── ActionArmsCompat.java
│   ├── ActionArmsConfig.java
│   └── mode/ShooterMode.java
├── client/                       # クライアント
│   ├── LMRBCompatClient.java
│   └── config/
│       ├── ConfigHubScreen.java
│       ├── ConfigScreenInfo.java
│       └── ConfigScreenManager.java
├── compat/                       # 互換フレームワーク
│   ├── AbstractCompat.java
│   └── CompatUtil.java
├── mode/                         # モード基底
│   └── AbstractShooterMode.java
├── reflection/                   # リフレクションユーティリティ
│   └── ReflectionUtil.java
├── fabric/                       # Fabric 固有
│   ├── LMRBCompatFabric.java
│   └── modmenu/LMRBCompatModMenu.java
└── forge/                        # Forge 固有
    ├── LMRBCompatForge.java
    ├── classicguns/              # ClassicGuns 互換
    ├── fn5728/                   # FN5728 互換
    ├── gvclib/                   # GVCLib 互換
    │   └── client/LMGunBaseFeatureRenderer.java
    ├── slashblade/               # SlashBlade 互換
    │   └── client/LMSlashBladeFeatureRenderer.java
    └── mixin/forge/              # Forge Mixin
        ├── gvclib/MixinMaidModelRenderer.java
        └── slashblade/
            ├── MixinAttackManager.java
            ├── MixinLittleMaidEntity.java
            └── MixinMaidModelRenderer.java
```

## 互換対応 Mod 一覧

| Mod | プラットフォーム | 武器タイプ | モード | 主な機能 |
|-----|----------------|-----------|--------|---------|
| ActionArms | Fabric/Forge | レバーアクション銃 | ShooterMode | GunController 連携、弾倉検知、残心システム |
| ClassicGuns | Forge | 汎用銃 | ShooterMode | 散弾、エンチャント対応、AR フルオート |
| FN5728 | Forge | FN Five-Seven / P90 | ShooterMode | P90 フルオート、弾丸拡散 |
| GVCLib | Forge | マガジン式銃 | ShooterMode | マガジンリロード、アタッチメント、弾種切替 |
| SlashBlade | Forge | 刀（近接） | SlashBladeMode | コンボ、集中ランクボーナス、2バージョン対応 |

## 主要パターン

### 1. リフレクション駆動の互換ロード

`loadCompat(modId, compatPath)` で動的にクラスをロード。
対象 Mod が存在しない場合はコンパイル時依存なしで安全にスキップ。

```java
CompatUtil.ifLoaded(modId, id ->
    ReflectionUtil.execWithInstancing(basePath + modId + "." + compatPath, "init"));
```

### 2. AbstractShooterMode の弾薬管理

2種類の弾薬システムを抽象メソッドで吸収:
- **マガジン方式** (GVCLib): `isMagazineReload() = true`、オフハンドの弾薬スタックを消費
- **カウント方式** (ClassicGuns, FN5728): `isMagazineReload() = false`、インベントリから個数消費

### 3. Mixin によるレンダラー注入

Mixin で `MaidModelRenderer` のコンストラクタをフックし、
リフレクション経由で `FeatureRenderer` を動的追加。Mod 検出と条件付き適用。

### 4. SlashBlade デュアルバージョン対応

Original と Resharped を実行時にクラス存在チェックで判定し、
別々の委譲クラス (`SlashBladeOriginal` / `SlashBladeResharped`) に処理を分岐。

### 5. 設定画面ハブ

`ConfigScreenManager` がシングルトンで全 Mod の設定画面を集約管理。
`ConfigHubScreen` で3列グリッド表示。AutoConfig + Cloth Config で GSON シリアライズ。

## プラットフォーム差異

| 観点 | Fabric | Forge |
|------|--------|-------|
| エントリポイント | `ModInitializer` | `@Mod` コンストラクタ |
| Mod 検出 API | `FabricLoader.getAllMods()` | `FMLLoader.getLoadingModList()` |
| 設定画面統合 | ModMenu API | `ConfigScreenHandler` |
| 互換 Mod 数 | 1 (ActionArms) | 5 (ActionArms + 4種) |
| Mixin | なし（空） | 4クラス |

## Mixin 一覧

| Mixin クラス | 対象 | 注入先 | 用途 |
|-------------|------|--------|------|
| `MixinLittleMaidEntity` | `LittleMaidEntity` | `tick()` HEAD | SlashBlade アイテムの毎 tick 更新 |
| `MixinAttackManager` | `AttackManager` | `doAttackWith()` HEAD | 集中ランクダメージボーナス適用 |
| `MixinMaidModelRenderer` (slashblade) | `MaidModelRenderer` | `<init>` RETURN | 刀レンダラー追加 |
| `MixinMaidModelRenderer` (gvclib) | `MaidModelRenderer` | `<init>` RETURN | 銃レンダラー追加 |

## 設定ファイル

| Mod | ファイル名 | 主要設定 |
|-----|-----------|---------|
| ActionArms | `lmrbcompat-actionarms.json` | `test: boolean` |
| ClassicGuns | `lmrbcompat-classicguns.json` | `shooterRangeFactor: float` |
| FN5728 | `lmrbcompat-fn5728.json` | `shooterRangeFactor: float` |
| GVCLib | `lmrbcompat-gvclib.json` | `shooterRangeFactor: float` |
| SlashBlade | `lmrbcompat-slashblade.json` | `test: boolean` |
