# 接続設計の分析と改善提案

## 背景

LMRBCompat の各 Mod との接続部分（互換ロード、リフレクション、Mixin）について、現行設計の問題点を洗い出し、より良い設計がないか検討する。

## 現行設計の分析

### 接続レイヤーの全体像

```
┌─────────────────────────────────────────────────────────┐
│ LMRBCompatForge / LMRBCompatFabric (エントリポイント)      │
│   loadCompat("modId", "CompatClass")                     │
│     └── ReflectionUtil.execWithInstancing(className, "init") │
└──────────────────────┬──────────────────────────────────┘
                       │ リフレクションで動的ロード
                       ▼
┌─────────────────────────────────────────────────────────┐
│ AbstractCompat<T extends ConfigData>                     │
│   init() → Config登録, ConfigScreen登録, ModeType登録     │
│   register(id, ModeType) → ModeManager に登録            │
└──────────────────────┬──────────────────────────────────┘
                       │
        ┌──────────────┼──────────────┐
        ▼              ▼              ▼
  [ShooterMode]  [SlashBladeMode]  [FeatureRenderer]
  (AbstractShooterMode)             (Mixin経由で注入)
```

### 接続ポイント別の分析

#### 1. 互換ロード: `loadCompat()` + `ReflectionUtil.execWithInstancing()`

**現状:**
```java
// LMRBCompatForge.java:42 のコメントが全てを物語っている
// "ハチャメチャなハードコードであるため、コードにエラーが出た場合でも無事起動できるようにする処置"
// "多分リフレクションは無くても良いかも？"
```

エントリポイントで `loadCompat("gvclib", "GVCLibCompat")` → リフレクションでクラス名を文字列結合 → `Class.forName()` でロード → デフォルトコンストラクタでインスタンス生成 → `init()` 呼び出し。

**問題点:**
- **リフレクションが不要な箇所で使われている**: `GVCLibCompat` は Forge モジュール内に存在し、コンパイル時に解決可能。Forge の `loadCompat()` は自モジュールのクラスをリフレクションで呼んでいる
- **クラス名の文字列結合が脆弱**: `basePath + modId + "." + compatPath` はリネーム・リファクタに追従しない
- **エラーが `printStackTrace()` で握り潰される**: 初期化失敗が無言で終わり、ユーザーにもログにも適切に伝わらない
- **modId とパッケージ名が暗黙的に一致している前提**: `"gvclib"` が modId であると同時にパッケージ名でもある

**リフレクションが本当に必要な場面:**
- `common/` の `LMRBCompat.init()` から ActionArms をロードする場合: ActionArms が存在しない環境では `ClassNotFoundException` を避ける必要がある → リフレクション必要
- `forge/` の `LMRBCompatForge` から Forge 専用 Compat をロードする場合: 対象 Mod が存在しない環境で `NoClassDefFoundError` を避ける必要がある → **ただし `CompatUtil.ifLoaded()` で既にガードしている**ため、Mod が存在する場合は直接参照可能

**実際には:** Forge モジュールは対象 Mod を `compileOnly` で依存しているため、クラスは常にコンパイル時に存在する。ランタイムで Mod が無い場合は `CompatUtil.ifLoaded()` のガードで到達しない。つまり **Compat クラス自体のロードにリフレクションは不要**。ただし Compat クラスのロード時に対象 Mod のクラスが classloader に無いと `NoClassDefFoundError` が発生するため、リフレクションが安全弁として機能している面はある。

#### 2. Compat 初期化: `AbstractCompat.init()`

**現状:**
各 Compat は `init()` で Config 登録 → ModeType 登録を一括で行う。

**問題点:**
- **INSTANCE パターンの不統一**: 全 Compat が `public static INSTANCE` を持つが、`init()` 内で代入。コンストラクタではなく `init()` で代入されるため、初期化順序に依存
- **Config と Mode 登録が密結合**: 設定だけ使いたい場合も ModeType 登録が必須

#### 3. ShooterMode 実装の重複

**現状:**
4つの ShooterMode（ActionArms, ClassicGuns, FN5728, GVCLib）が `AbstractShooterMode` を継承し、それぞれ約15個の抽象メソッドを実装。

**問題点:**
- **音声再生ロジックの重複**: `playReloadStartSound()`, `playShootSound()` が4箇所でほぼ同じ `ForgeRegistries.SOUND_EVENTS.getValue()` + `world.playSound()` パターン
- **パーティクルエフェクトの重複**: `shootEffect()` が複数箇所で銃口位置計算 + パーティクル生成
- **弾薬消費ロジック (`consumeAmmo()`)**: `AbstractShooterMode` 内の130行超のメソッドが magazine/count 両方式を内包。条件分岐が複雑
- **GVCLib の `fireBullet()` が巨大**: 1メソッド130行超、音声・弾生成・リコイル・マズルフラッシュが全て同一メソッド内

#### 4. SlashBlade デュアルバージョン対応

**現状:**
`SlashBladeCompat.slashInput()` が `ReflectionUtil.isClassExist()` でバージョン判定し、`SlashBladeOriginal` か `SlashBladeResharped` のどちらかをリフレクション経由で呼ぶ。

**問題点:**
- **2つのクラスの実装が95%同一**: `SlashBladeOriginal` と `SlashBladeResharped` の差は戻り値の比較部分のみ（`ComboState.NONE` vs `ComboStateRegistry.NONE.getId()`）
- **呼び出しごとにリフレクション**: `slashInput()` は攻撃のたびに呼ばれるが、毎回 `execStatic()` でメソッドを解決している
- **`progressCombo()` のリフレクション**: コメントによると `ISlashBladeState` が default メソッドのためメソッド解決に失敗する。これは Java のリフレクションで interface の default メソッドを扱う際の既知問題

#### 5. Mixin による FeatureRenderer 注入

**現状:**
`MixinMaidModelRenderer` が2つ（slashblade 用・gvclib 用）あり、どちらもコンストラクタ末尾で `ReflectionUtil.getConstructor()` 経由で FeatureRenderer を追加。

**問題点:**
- **2つの Mixin クラスがほぼ同一**: modId と FeatureRenderer クラス名が異なるだけ
- **同一クラスへの複数 Mixin**: `MaidModelRenderer` に2つの Mixin が適用されるが、Mixin の適用順序は保証されない（現状は問題ないが脆い）
- **FeatureRenderer のロードにリフレクション**: Mixin クラス自体が対象 Mod の存在に依存しないため、`CompatUtil.isModLoaded()` ガード + 直接参照でも可能

#### 6. `MixinAttackManager` (SlashBlade)

**現状:**
`AttackManager.doAttackWith()` の HEAD で inject し、メイドさん用のダメージ計算ロジック（集中ランクボーナス）を差し込んで `ci.cancel()` で元の処理をキャンセル。

**問題点:**
- **全エンティティに影響**: メイドさん以外の攻撃にも Mixin が適用される。`src.getAttacker()` がメイドさんかどうかのチェックがない
- **元メソッドの完全置換**: `ci.cancel()` で元のロジックを丸ごとキャンセルするため、SlashBlade 側の更新に追従しにくい
- **ダメージ計算の再実装**: SlashBlade の本来のダメージ計算をこちらで再実装しているため、SlashBlade 更新時に壊れるリスクが高い

#### 7. `MixinLittleMaidEntity` (SlashBlade)

**現状:**
`tick()` の HEAD で SlashBlade アイテムの `inventoryTick()` を手動呼び出し。

**問題点:**
- **SlashBlade 未導入時もチェックが走る**: 毎 tick `CompatUtil.isModLoaded("slashblade")` が呼ばれる（Set の contains なので軽いが無駄）
- **インベントリ二重走査**: 全スロット走査後にメインハンド・オフハンドを別途処理。一度のループで済むはず

## 改善提案

### A. リフレクション削減 — try-catch ガードパターン

**優先度: 高 / リスク: 低**

Compat ロードのリフレクションを、try-catch による `NoClassDefFoundError` ガードに置換:

```java
// Before
private void loadCompat(String modId, String compatPath) {
    String basePath = "net.sistr.lmrbcompat.forge.";
    CompatUtil.ifLoaded(modId, id ->
        ReflectionUtil.execWithInstancing(basePath + modId + "." + compatPath, "init"));
}

// After
private void loadCompat(String modId, Supplier<AbstractCompat<?>> factory) {
    CompatUtil.ifLoaded(modId, id -> {
        try {
            factory.get().init();
        } catch (NoClassDefFoundError e) {
            LOGGER.warn("Failed to load compat for {}: {}", modId, e.getMessage());
        }
    });
}

// 呼び出し側
loadCompat("gvclib", GVCLibCompat::new);
loadCompat("slashblade", SlashBladeCompat::new);
```

**メリット:**
- コンパイル時の型チェックが効く
- IDE でのリファクタリングに追従する
- エラーが適切にログされる

**注意:** `Supplier` のラムダが評価される時点で Compat クラスがロードされ、その時点で対象 Mod のクラスが必要。`CompatUtil.ifLoaded()` で Mod 存在確認済みでも、classloader に読み込まれていない場合は `NoClassDefFoundError` が出る可能性がある。ただし Forge/Fabric のモジュールシステムでは読み込み済み Mod のクラスは利用可能なので、通常は問題ない。

### B. SlashBlade バージョン分岐の改善 — Strategy キャッシュ

**優先度: 高 / リスク: 低**

初期化時にバージョン判定し、適切な Strategy をキャッシュ:

```java
// インターフェース
@FunctionalInterface
interface SlashBladeInput {
    boolean slashInput(ItemStack stack, LivingEntity mob, boolean isR);
}

// SlashBladeCompat 内
private SlashBladeInput inputStrategy;

public void init() {
    super.init();
    // 初期化時に一度だけ判定
    if (ReflectionUtil.isClassExist(
            "mods.flammpfeil.slashblade.capability.slashblade.ComboState")) {
        inputStrategy = SlashBladeOriginal::slashInput;
    } else {
        inputStrategy = SlashBladeResharped::slashInput;
    }
}

public boolean slashInput(ItemStack stack, LivingEntity mob, boolean isR) {
    return inputStrategy.slashInput(stack, mob, isR);
}
```

**メリット:**
- 毎回のリフレクション呼び出しを排除
- `slashInput()` は攻撃ごとに呼ばれるためパフォーマンス改善

### C. Mixin 統合 — 単一 MixinMaidModelRenderer

**優先度: 中 / リスク: 低**

2つのほぼ同一な `MixinMaidModelRenderer` を1つに統合:

```java
@Mixin(MaidModelRenderer.class)
public abstract class MixinMaidModelRenderer extends MobEntityRenderer<...> {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(EntityRendererFactory.Context ctx, CallbackInfo ci) {
        addFeatureIfLoaded("slashblade",
            "net.sistr.lmrbcompat.forge.slashblade.client.LMSlashBladeFeatureRenderer");
        addFeatureIfLoaded("gvclib",
            "net.sistr.lmrbcompat.forge.gvclib.client.LMGunBaseFeatureRenderer");
    }

    @Unique
    private void addFeatureIfLoaded(String modId, String className) {
        if (!CompatUtil.isModLoaded(modId)) return;
        ReflectionUtil.getConstructor(className, FeatureRendererContext.class)
            .map(c -> c.newInstance(this))
            .flatMap(o -> o)
            .filter(o -> o instanceof FeatureRenderer)
            .map(o -> (FeatureRenderer) o)
            .ifPresent(this::addFeature);
    }
}
```

**メリット:**
- 重複コード削減
- 新しい FeatureRenderer 追加が1行で済む
- Mixin 適用順序の問題を回避

### D. MixinAttackManager の安全性向上

**優先度: 高 / リスク: 中**

メイドさん以外への影響を排除:

```java
@Inject(method = "doAttackWith", at = @At("HEAD"), cancellable = true)
private static void onDoAttackWith(DamageSource src, float amount,
        Entity target, boolean forceHit, boolean resetHit, CallbackInfo ci) {
    // メイドさん以外の攻撃は元のロジックに任せる
    if (!(src.getAttacker() instanceof LittleMaidEntity)) {
        return;
    }
    // ... 以降の処理
}
```

### E. 音声再生・エフェクトのユーティリティ化

**優先度: 低 / リスク: 低**

4箇所で重複する音声再生パターンをユーティリティに:

```java
public class SoundUtil {
    public static void playAtEntity(LivingEntity entity, String modId,
            String soundName, float volume, float pitch) {
        var soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(
            Identifier.of(modId, soundName));
        if (soundEvent == null) return;
        entity.getWorld().playSound(null,
            entity.getX(), entity.getY(), entity.getZ(),
            soundEvent, SoundCategory.NEUTRAL, volume, pitch);
    }
}
```

### F. consumeAmmo() の分割

**優先度: 低 / リスク: 中**

130行の `consumeAmmo()` を magazine/count 方式別にクラス分割するか、Strategy パターンに:

```java
interface AmmoStrategy {
    void consumeAmmo(LittleMaidEntity maid, AbstractShooterMode<?> mode);
}

class MagazineAmmoStrategy implements AmmoStrategy { ... }
class CountAmmoStrategy implements AmmoStrategy { ... }
```

ただしこれは現行でも `isMagazineReload()` の分岐で動作しており、実害は薄い。

## 改善の優先順位

| 優先度 | 提案 | 工数 | 効果 |
|--------|------|------|------|
| 1 | D. MixinAttackManager 安全性 | 小 | バグ防止（全エンティティ影響を排除） |
| 2 | A. リフレクション削減 | 小 | 保守性・型安全性の大幅向上 |
| 3 | B. SlashBlade Strategy キャッシュ | 小 | パフォーマンス改善、コード明瞭化 |
| 4 | C. Mixin 統合 | 小 | 重複削除、拡張性向上 |
| 5 | E. 音声ユーティリティ | 小 | 重複削減（ただし実害は薄い） |
| 6 | F. consumeAmmo 分割 | 中 | 可読性向上（ただしリスクあり） |

## 結論

最も効果が高い改善は **A（リフレクション削減）と D（MixinAttackManager の安全性）**。どちらも工数が小さく、既存の動作を壊すリスクが低い。

現行のリフレクション多用は「ハチャメチャなハードコード」（コード内コメントより）の時代の名残であり、`CompatUtil.ifLoaded()` が整備された現在では大部分が不要。型安全な接続に移行することで、リファクタリング耐性と IDE サポートが大幅に向上する。

ただし **SlashBladeOriginal/Resharped 内の `progressCombo()` リフレクション**は、Java の interface default メソッドのリフレクション問題に起因するため、リフレクション以外の回避手段が限られる。ここは現行のままが妥当。
