# 専用サーバーで設定画面登録時にクラッシュする問題の調査と修正

- 日付: 2026-05-30
- 対象: LMRBCompat 1.20.1
- リリース: v1.3.1（v1.3.0 のクラッシュを修正）
- 関連コミット: `598b081`（fix）, `1e7a07b`（version bump）

## 発端（報告内容）

> 「SlashBlade: Resharped が新しいバージョンだとバグるっぽい」

報告者は SlashBlade Resharped のバージョンを上げたところクラッシュした、と認識していた。
LMRBCompat が想定していた Resharped は `1.2.30`（`forge/no_load_mods/` に同梱）で、
報告者の環境は最新の `1.9.65` だった。

## 調査経過

### 1. SlashBlade Resharped の API 差分を検証（1.2.30 → 1.9.65）

LMRBCompat が直接参照している API を新旧 jar で `javap` 比較した結果、**全てシグネチャ一致**。

| 参照 API | 結果 |
|---|---|
| `ItemSlashBlade.BLADESTATE` / `INPUT_STATE` | 同一 |
| `InputCommand.R_CLICK` / `L_CLICK` | 同一 |
| `ComboStateRegistry.NONE`（`RegistryObject<ComboState>`） | 同一 |
| `IInputState.getCommands()` | 同一 |
| `ISlashBladeState.progressCombo(LivingEntity)` | シグネチャ同一・中身のみ変化 |
| `LayerMainBlade(RenderLayerParent)`（レンダラ継承元） | 同一 |
| `ISlashBladeState.isBroken()` | 同一 |

唯一の挙動変化は `progressCombo` が新規イベント `SlashBladeEvent$PerformSlashArtEvent` を
Forge EVENT_BUS に post するようになった点。当初これを原因と仮説したが、**外れだった**。

### 2. クラッシュレポートで真因が判明

報告者のクラッシュレポート（`crash-2026-05-13_20.50.26-fml.txt`）は **専用サーバー起動時の
mod loading error** だった。

```
java.lang.RuntimeException: Attempted to load class
net/minecraft/client/gui/screens/Screen for invalid dist DEDICATED_SERVER
```

スタックトレース:

```
LMRBCompatForge.onCommonSetup(:48)
 → LMRBCompat.loadCompat(:19)
  → SlashBladeCompat.init(:31)   ← super.init()
   → AbstractCompat.init(:30)    ← ここでクライアント専用クラスをロード
    → RuntimeDistCleaner が Screen を拒否
```

## 根本原因

`AbstractCompat.init()`（common・`common_setup` で専用サーバーでも実行される）が、
設定画面の登録を無条件に行っていた:

```java
ConfigScreenManager.getINSTANCE()
    .register(uniqueID, ConfigScreenInfo.of(name, key,
        screen -> AutoConfig.getConfigScreen(configClass, screen).get()));
```

`screen -> ...` ラムダは `Function<Screen, Screen>` を生成する。この **invokedynamic の
ブートストラップ時に `net.minecraft.client.gui.screen.Screen`（クライアント専用）が
ロードされ**、専用サーバー上で `RuntimeDistCleaner` に弾かれてクラッシュしていた。

### SlashBlade のバージョンとは無関係

- クラッシュは `super.init()`（`SlashBladeCompat.java:31`）の中、つまり SlashBlade の
  バージョン判定（`SlashBladeCompat.java:35-40`）に**到達する前**で発生。
- スタックに SlashBlade が出るのは、報告者のサーバーに入っている compat 対象 mod が
  SlashBlade だけで、`loadCompat` のうち実際に `init()` が走る最初の compat が
  SlashBlade だったため（バグは全 compat 共通の `AbstractCompat` 側）。
- よって SlashBlade を旧版に戻しても直らない。トリガは「専用サーバーで動かしたこと」。

## 修正

画面登録ロジックを **client 専用クラス `ConfigScreenRegistrar` へ隔離**し、
`init()` 本体から `Screen` 参照ラムダを排除。クライアント環境でのみ呼び出す。

```java
// AbstractCompat.init()
AutoConfig.register(configClass, GsonConfigSerializer::new);
CONFIG_HOLDER = AutoConfig.getConfigHolder(configClass);
if (Platform.getEnvironment() == Env.CLIENT) {
    ConfigScreenRegistrar.register(getUniqueID(), getName(), configClass);
}
```

- `ConfigScreenRegistrar.register(String, String, Class)` の引数型は全て server-safe。
  ガード（`if_acmpne`）で囲まれているため、専用サーバーでは `ConfigScreenRegistrar`・
  `Screen` ともにロードされない。
- あわせて `LMRBCompatForge` の `registerExtensionPoint`（client 専用
  `ConfigScreenHandler` を参照）も既存の `if (FMLEnvironment.dist.isClient())` ブロック内へ移動。

### 横展開チェック（他の画面登録）

| 箇所 | 判定 | 対応 |
|---|---|---|
| `AbstractCompat.init`（common） | クラッシュ源 | 修正 |
| `LMRBCompatForge` の `registerExtensionPoint` | client ガード外で脆弱（supplier 遅延で当時は未発症） | isClient ガード内へ移動 |
| `LMRBCompatModMenu`（fabric ModMenu entrypoint） | client 専用 entrypoint なので安全 | 変更なし |
| 各 compat サブクラス | 画面登録は `AbstractCompat.init` 継承で独自コードなし | 自動的に修正の恩恵 |

## 検証

- `:common:compileJava` `:forge:compileJava` `:fabric:compileJava` 全て成功。
- `AbstractCompat.class` のバイトコードを `javap -c` で確認:
  `Screen` / `invokedynamic(Function<Screen,Screen>)` / `ConfigScreenInfo` /
  `getConfigScreen` の参照がクラス内から完全に消滅。画面登録はガード付き
  `invokestatic ConfigScreenRegistrar.register(...)` のみ。
- Windows 側で `:forge:runServer`（SlashBlade を入れた専用サーバー）起動テスト成功。

## 教訓

- **「新バージョンでバグる」という報告のバージョン因果を鵜呑みにしない。** 実際のトリガは
  サーバー実行であり、SlashBlade バージョンは無関係だった。クラッシュレポートを取得して
  初めて切り分けられた。
- Architectury の common コードは client/server 両方で実行される。`Screen` などクライアント
  専用クラスを参照するコード（特に **ラムダの invokedynamic**）は、ガードしても
  メソッド本体に残すとブートストラップでロードされうる。**client 専用クラスへ物理的に隔離**し、
  共通メソッド本体から参照を消すのが確実。
- サイド判定は common では `dev.architectury.platform.Platform.getEnvironment() == Env.CLIENT`
  を使う（`FMLEnvironment` は Forge 限定）。
