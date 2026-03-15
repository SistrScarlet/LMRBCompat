# ActionArms AIGunController API (v1.2.0-beta)

## 概要

ActionArms 1.2.0-beta で追加された AI 用銃操作 API。
銃の種類（レバーアクション、SAA 等）を意識せず、統一インターフェースで銃を操作できる。

## API 一覧

### GunItem インターフェース

```java
import net.sistr.actionarms.item.GunItem;

// ItemStack から GunItem を取得
if (stack.getItem() instanceof GunItem gunItem) {
    // AIGunController を生成
    AIGunController controller = gunItem.createAIController(
        entity,                                    // LivingEntity: 銃を使うエンティティ
        () -> entity.getMainHandStack(),           // Supplier<ItemStack>: 銃の ItemStack
        () -> Optional.of(entity.getInventory())); // Supplier<Optional<Inventory>>: 弾薬インベントリ
}
```

- `GunItem` は `Item` ではなくインターフェース。`instanceof` チェックで使用する
- `LeverActionGunItem`, `SAAGunItem` 等が実装している
- 旧 API の `LeverActionGunItem` クラス直接参照は不要になる

### AIGunController インターフェース

```java
import net.sistr.actionarms.entity.util.AIGunController;
import net.sistr.actionarms.entity.util.AIGunController.GunGoal;
import net.sistr.actionarms.entity.util.AIGunController.GunStatus;
import net.sistr.actionarms.entity.util.AIGunController.TickResult;
```

#### メソッド

| メソッド | 説明 |
|---------|------|
| `void setGoal(GunGoal goal)` | AI の行動目標を設定 |
| `GunGoal getGoal()` | 現在の目標を取得 |
| `void setCooldownMultiplier(float)` | 操作クールダウン倍率（デフォルト 1.0） |
| `GunStatus getStatus()` | 現在の状態を取得（判断材料） |
| `TickResult tick(float timeDelta)` | 毎 tick 呼び出す。`timeDelta` は通常 `1f / 20f` |

#### GunGoal

| 値 | 説明 | 内部動作 |
|----|------|---------|
| `IDLE` | 何もしない | コンポーネントの tick のみ（タイマー進行） |
| `RELOAD` | リロードする | マガジン/シリンダーに弾を込める |
| `READY` | 撃てる状態にする | コック、必要ならリロードも行う |
| `ATTACK` | 攻撃する | READY + 射撃 |

- Goal は毎 tick 設定し続ける。設定しなければ前回の Goal が維持される
- 不可能な Goal を設定してもエラーにはならない（何も起きない）

#### GunStatus

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `canAttack` | `boolean` | ATTACK 行動が取れるか。弾が完全に無ければ false |
| `canReload` | `boolean` | RELOAD 行動が取れるか。満タン or 弾無しなら false |

#### TickResult

| フィールド | 型 | 説明 |
|-----------|-----|------|
| `fired` | `boolean` | この tick で射撃が発生したか |

- `fired` を使ってメイドさんの `LMSounds.SHOOT` 再生やハンドスイングを行う

### 操作クールダウン

各操作後に AI 側のクールダウンが発生する。銃本体の cooldown/phaseTimer とは別。
メイドさんが達人並みの操作をしないようにするための仕組み。

```java
// 遅め（のんびり操作）
controller.setCooldownMultiplier(2.0f);

// 速め（手慣れた操作）
controller.setCooldownMultiplier(0.5f);
```

ベースクールダウン（tick 数、参考値。調整中）:

| 操作 | LeverAction | SAA |
|------|------------|-----|
| 射撃 | 5 | 5 |
| サイクル/コック | 3 | 3 |
| リロード | 3 | - |
| ゲート開閉 | - | 3 |
| 排莢 | - | 2 |
| 装填 | - | 2 |

## 移行ガイド: 既存 ShooterMode → AIGunController

### 変更前（現在の実装）

```
ShooterMode
├── GunController + KeyInputManager を直接使用
├── LeverActionGunItem 専用（SAAGunItem 非対応）
├── キー入力シミュレーション（5tick間隔）
├── 銃コンポーネントの状態を直接読み取り
└── shouldCycle, shouldReload, canTrigger 等を個別判定
```

### 変更後

```
ShooterMode
├── AIGunController を使用（GunItem.createAIController）
├── 全銃種対応（LeverAction, SAA, 将来の銃も自動対応）
├── Goal 設定のみ（ATTACK / RELOAD / READY / IDLE）
├── GunStatus で判断（canAttack, canReload）
└── TickResult.fired() で射撃検知
```

### 実装例

```java
public class ShooterMode extends AbstractArcherMode<Item> {
    private AIGunController aiGun;
    private int inSightTime;
    private final double maxAimDegreesCos = Math.cos(Math.toRadians(15));
    private int zanshin;

    public ShooterMode(ModeType<...> modeType, String name, LittleMaidEntity mob) {
        super(modeType, name, mob);
    }

    @Override
    public boolean shouldExecute() {
        zanshin = Math.max(0, zanshin - 1);

        var stack = this.mob.getMainHandStack();
        if (!(stack.getItem() instanceof GunItem gunItem)) {
            return false;
        }

        // AIGunController の生成（初回 or 銃が変わった場合）
        if (aiGun == null) {
            aiGun = gunItem.createAIController(
                mob,
                () -> mob.getMainHandStack(),
                () -> Optional.of(mob.getInventory()));
            aiGun.setCooldownMultiplier(1.5f); // メイドさんは少しゆっくり
        }

        var status = aiGun.getStatus();
        if (!status.canAttack() && !status.canReload()) {
            return false;
        }

        boolean shouldExecute = super.shouldExecute();
        if (shouldExecute) {
            return true;
        }
        return zanshin > 0;
    }

    @Override
    public void tick() {
        var target = this.mob.getTarget();
        boolean targetAlive = target != null && target.isAlive();
        var status = aiGun.getStatus();

        // Goal 決定
        if (targetAlive && status.canAttack()) {
            aiGun.setGoal(GunGoal.ATTACK);
            zanshin = 10;
        } else if (status.canReload()) {
            aiGun.setGoal(GunGoal.RELOAD);
            zanshin = 10;
        } else {
            aiGun.setGoal(GunGoal.IDLE);
        }

        // ターゲットがいる場合は親の tick で視線追従
        if (targetAlive) {
            super.tick();
        }

        // 銃の tick
        var result = aiGun.tick(1f / 20f);

        // 射撃時の演出
        if (result.fired()) {
            this.mob.play(LMSounds.SHOOT);
            this.mob.swingHand(Hand.MAIN_HAND);
        }
    }

    @Override
    protected void tickRangedAttack(
            LivingEntity target, ItemStack stack,
            boolean canSee, double distanceSq, float maxRange) {
        if (canSee) {
            inSightTime++;
        } else {
            inSightTime = 0;
        }

        // 視界に入って間もない or 射程外 → READY に留める
        if (inSightTime < 10 || distanceSq >= maxRange * maxRange) {
            aiGun.setGoal(GunGoal.READY);
            return;
        }

        // 照準チェック
        var lookFor = this.mob.getRotationVec(1.0f);
        var targetFor = target.getEyePos().subtract(this.mob.getEyePos()).normalize();
        if (targetFor.dotProduct(lookFor) < maxAimDegreesCos) {
            aiGun.setGoal(GunGoal.READY);
            return;
        }

        // 射線チェック（味方への誤射防止）
        var rayResult = this.raycastShootLine(
            target, maxRange,
            e -> e instanceof LivingEntity living && this.mob.isFriend(living));
        if (rayResult.isPresent() && rayResult.get().getType() != HitResult.Type.MISS) {
            aiGun.setGoal(GunGoal.READY);
            return;
        }

        // 射撃 OK → ATTACK（setGoal は tick() 側で既に設定済みだが、明示的に）
        aiGun.setGoal(GunGoal.ATTACK);
    }

    @Override
    protected float getMaxRange(ItemStack itemStack) {
        return 16;
    }

    @Override
    protected Optional<Item> getWeaponInstance(ItemStack itemStack) {
        if (itemStack.getItem() instanceof GunItem) {
            return Optional.of(itemStack.getItem());
        }
        return Optional.empty();
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.zanshin = 0;
        this.inSightTime = 0;
        if (aiGun != null) {
            aiGun.setGoal(GunGoal.IDLE);
        }
    }
}
```

### ActionArmsCompat の変更

```java
public void init() {
    super.init();
    INSTANCE = this;
    register(
        "shooter",
        ModeType.<ShooterMode>builder(
                (type, entity) -> new ShooterMode(type, "Shooter", entity))
            .addItemMatcher(
                ItemMatchers.clazz(GunItem.class),      // LeverActionGunItem → GunItem に変更
                ItemMatcher.Priority.HIGH)
            .build());
}
```

### 主な変更点まとめ

| 項目 | 旧 | 新 |
|------|-----|-----|
| 対応銃 | `LeverActionGunItem` のみ | `GunItem` 全般 |
| ItemMatcher | `ItemMatchers.clazz(LeverActionGunItem.class)` | `ItemMatchers.clazz(GunItem.class)` |
| 操作方法 | `KeyInputManager` でキー入力シミュレーション | `AIGunController.setGoal()` |
| 状態取得 | `LeverActionGunComponent` を直接読み取り | `AIGunController.getStatus()` |
| 射撃検知 | chamber 状態の差分比較 | `TickResult.fired()` |
| 弾切れ判定 | `chamber.canShoot() && magazine.isEmpty() && !hasBullet()` | `!status.canAttack()` |
| 新銃追加時 | LMRBCompat 側の変更が必要 | 不要（ActionArms 側で完結） |

## 注意事項

- `AIGunController` はサーバー側専用。`tick()` はクライアント側で呼ぶと何もしない
- `getStatus()` は NBT 読み取りを伴うため、1 tick に何度も呼ばない方が良い
- `GunItem` はインターフェースなので `ItemMatchers.clazz()` で使用可能（instanceof チェック）
- クールダウンのベース値は調整中（beta で要テスト）
