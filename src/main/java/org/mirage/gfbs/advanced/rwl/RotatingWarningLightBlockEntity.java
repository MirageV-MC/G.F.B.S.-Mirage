package org.mirage.gfbs.advanced.rwl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.mirage.gfbs.Objects.ModBlockEntities;

/**
 * 聚光灯引擎数据源（BlockEntity）：
 * - 不再使用原版方块光照（不再放置 minecraft:light）
 * - 只维护并同步“聚光灯参数”（位置/方向/颜色/锥角/强度/衰减/高光等）
 * - 渲染端（你的自定义着色器/后处理/延迟渲染器）从这里取参数做完全自定义光照
 */
public class RotatingWarningLightBlockEntity extends BlockEntity {

    // ===== 用户配置 / 同步数据 =====
    private int colorR = 255;
    private int colorG = 64;
    private int colorB = 64;

    private String soundId = "minecraft:block.note_block.bell";
    private long msPerRevolution = 1200L;

    private boolean powered = false;

    /** 旋转计时基准（用于客户端按同样公式还原角度） */
    private long startGameTime = 0L;
    /** 断电时锁定的角度（用于恢复/断电保持） */
    private float startAngleDeg = 0.0f;

    // ===== 聚光灯引擎参数（全部会同步到客户端）=====
    private float spotMaxDist = 16.0f;           // 影响半径
    private float spotHalfAngleDeg = 18.0f;      // 半锥角（度）
    private float spotIntensity = 0.95f;         // 强度（0~?）
    private float spotSoftness = 0.10f;          // 边缘羽化宽度（0~1）
    private float spotSpecularStrength = 0.60f;  // 高光强度
    private float spotShininess = 48.0f;         // 高光锐度

    // NBT keys（放在同一 root 下，避免散落）
    private static final String NBT_POWERED = "powered";
    private static final String NBT_START_GT = "startGameTime";
    private static final String NBT_START_ANGLE = "startAngleDeg";

    private static final String NBT_SPOT_MAX_DIST = "spotMaxDist";
    private static final String NBT_SPOT_HALF_ANGLE = "spotHalfAngleDeg";
    private static final String NBT_SPOT_INTENSITY = "spotIntensity";
    private static final String NBT_SPOT_SOFTNESS = "spotSoftness";
    private static final String NBT_SPOT_SPEC = "spotSpecularStrength";
    private static final String NBT_SPOT_SHINE = "spotShininess";

    public RotatingWarningLightBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.RWL_ENTITY.get(), pos, state);
    }

    public RotatingWarningLightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null) return;

        if (level.isClientSide()) {
            org.mirage.gfbs.advanced.rwl.client.RWLClientSoundRegistry.onLoad(this);
            org.mirage.gfbs.advanced.rwl.client.RWLClientSpotlightRegistry.onLoad(this);
            return;
        }

        RWLServerRegistry.onLoad(this);
        if (level instanceof ServerLevel sl) {
            RWLLevelState st = RWLLevelState.get(sl);
            st.applyTo(sl, this, true);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            if (level.isClientSide()) {
                org.mirage.gfbs.advanced.rwl.client.RWLClientSoundRegistry.onRemove(this);
                org.mirage.gfbs.advanced.rwl.client.RWLClientSpotlightRegistry.onRemove(this);
            } else {
                RWLServerRegistry.onRemove(this);
            }
        }
        super.setRemoved();
    }

    // ===== 配置同步入口（服务器定期下发/GUI 改参数时调用） =====

    public boolean applyFromLevelState(ServerLevel sl, RWLLevelState st, boolean updateBlockStatePowered) {
        boolean changed = false;

        int nr = st.getColorR();
        int ng = st.getColorG();
        int nb = st.getColorB();
        String ns = st.getSoundId();
        long nms = st.getMsPerRevolution();
        boolean np = st.isEnabled();

        if (this.colorR != nr || this.colorG != ng || this.colorB != nb || this.msPerRevolution != nms || !safeEq(this.soundId, ns)) {
            this.colorR = nr;
            this.colorG = ng;
            this.colorB = nb;
            this.msPerRevolution = nms;
            this.soundId = ns;
            changed = true;
        }

        if (this.powered != np) {
            onPowerChanged(np);
            changed = true;
        }

        if (updateBlockStatePowered) {
            BlockState bs = this.getBlockState();
            if (bs.hasProperty(RotatingWarningLightBlock.POWERED) && bs.getValue(RotatingWarningLightBlock.POWERED) != np) {
                sl.setBlock(worldPosition, bs.setValue(RotatingWarningLightBlock.POWERED, np), 3);
                changed = true;
            }
        }

        if (changed) markUpdated();
        return changed;
    }

    private static boolean safeEq(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private void markUpdated() {
        this.setChanged();
        if (level instanceof ServerLevel sl) {
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void setConfig(int r, int g, int b, String sound, long msPerRev) {
        this.colorR = RWLApi.clamp255(r);
        this.colorG = RWLApi.clamp255(g);
        this.colorB = RWLApi.clamp255(b);
        if (sound != null && !sound.isEmpty()) this.soundId = sound;
        this.msPerRevolution = RWLApi.clampMs(msPerRev);
        markUpdated();
    }

    /**
     * 供你的“聚光灯引擎”/GUI 调整灯光参数（会同步到客户端）
     */
    public void setSpotlightParams(float maxDist, float halfAngleDeg, float intensity, float softness, float specularStrength, float shininess) {
        this.spotMaxDist = clampF(maxDist, 0.1f, 128.0f);
        this.spotHalfAngleDeg = clampF(halfAngleDeg, 0.1f, 89.0f);
        this.spotIntensity = clampF(intensity, 0.0f, 8.0f);
        this.spotSoftness = clampF(softness, 0.0f, 1.0f);
        this.spotSpecularStrength = clampF(specularStrength, 0.0f, 8.0f);
        this.spotShininess = clampF(shininess, 1.0f, 512.0f);
        markUpdated();
    }

    private static float clampF(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ===== 电源切换：只影响旋转基准 + 声音（不再做原版光照） =====

    public void onPowerChanged(boolean poweredNow) {
        if (this.level == null) {
            this.powered = poweredNow;
            return;
        }

        long gt = this.level.getGameTime();

        if (poweredNow) {
            if (!this.powered) {
                this.startGameTime = gt;
            }
        } else {
            if (this.powered) {
                this.startAngleDeg = calcAngleDegAtTick(gt);
                this.startGameTime = gt;
            }
        }

        this.powered = poweredNow;

        if (level.isClientSide()) {
            org.mirage.gfbs.advanced.rwl.client.RWLClientSoundRegistry.onPowerChange(this, poweredNow);
        }
    }

    // ===== getters（渲染端/引擎端读取） =====

    public boolean isPoweredCached() { return powered; }

    public int getColorR() { return colorR; }
    public int getColorG() { return colorG; }
    public int getColorB() { return colorB; }

    public String getSoundId() { return soundId; }
    public long getMsPerRevolution() { return msPerRevolution; }

    public long getStartGameTime() { return startGameTime; }
    public float getStartAngleDeg() { return startAngleDeg; }

    public float getSpotMaxDist() { return spotMaxDist; }
    public float getSpotHalfAngleDeg() { return spotHalfAngleDeg; }
    public float getSpotIntensity() { return spotIntensity; }
    public float getSpotSoftness() { return spotSoftness; }
    public float getSpotSpecularStrength() { return spotSpecularStrength; }
    public float getSpotShininess() { return spotShininess; }

    public float getSpotAngleCos() {
        return (float) Math.cos(Math.toRadians(spotHalfAngleDeg));
    }

    // ===== 角度计算（客户端/服务器共用公式） =====

    public float calcAngleDegAtTick(long gameTimeTick) {
        long msPerRev = Math.max(50L, this.msPerRevolution);
        float gameTime = (gameTimeTick - this.startGameTime);
        float elapsedMs = gameTime * 50.0f;
        float t = (elapsedMs % msPerRev) / (float) msPerRev;
        return (this.startAngleDeg + t * 360.0f) % 360.0f;
    }

    /**
     * 客户端渲染用角度（partialTicks 平滑）
     */
    public float calcAngleDegClient(float partialTicks) {
        if (level == null) return startAngleDeg;
        if (!powered) return startAngleDeg;

        long msPerRev = Math.max(50L, this.msPerRevolution);
        long gt = level.getGameTime();
        float gameTime = (gt - this.startGameTime) + partialTicks;
        float elapsedMs = gameTime * 50.0f;
        float t = (elapsedMs % msPerRev) / (float) msPerRev;
        return (this.startAngleDeg + t * 360.0f) % 360.0f;
    }

    // ====== 聚光射线计算（提供给“聚光灯引擎”喂参数） ======

    public static final class SpotRay {
        public final Vec3 start;
        public final Vec3 end;
        public final Vec3 dir;

        public SpotRay(Vec3 start, Vec3 end, Vec3 dir) {
            this.start = start;
            this.end = end;
            this.dir = dir;
        }
    }

    /**
     * 计算灯头出射射线（世界坐标）。
     * start 会被沿方向推进一点，避免立即命中自身方块。
     */
    public static SpotRay computeSpotRay(BlockPos bePos, BlockState state, float angleDeg, double maxDist) {
        Matrix4f mat = new Matrix4f().identity();
        mat.translate(bePos.getX(), bePos.getY(), bePos.getZ());

        applyMountTransform(state, mat);

        // 与渲染一致：灯头位置 + 旋转
        mat.translate(0.5f, 0.42f, 0.5f);
        mat.rotateY((float) Math.toRadians(angleDeg));
        mat.translate(0.0f, 0.10f, 0.0f);

        Vector3f p0 = new Vector3f(0.0f, 0.06f, 0.20f);
        Vector3f p1 = new Vector3f(0.0f, 0.06f, 1.20f);

        mat.transformPosition(p0);
        mat.transformPosition(p1);

        Vec3 start0 = new Vec3(p0.x, p0.y, p0.z);
        Vec3 dir = new Vec3(p1.x - p0.x, p1.y - p0.y, p1.z - p0.z).normalize();

        Vec3 start = start0.add(dir.scale(0.60));
        Vec3 end = start.add(dir.scale(maxDist));

        return new SpotRay(start, end, dir);
    }

    /**
     * 客户端/引擎侧常用：直接按当前状态 + partialTicks 计算本帧射线
     */
    public SpotRay computeSpotRayClient(float partialTicks) {
        float angle = calcAngleDegClient(partialTicks);
        return computeSpotRay(this.worldPosition, this.getBlockState(), angle, this.spotMaxDist);
    }

    private static void applyMountTransform(BlockState state, Matrix4f mat) {
        AttachFace face = state.getValue(RotatingWarningLightBlock.FACE);
        Direction facing = state.getValue(RotatingWarningLightBlock.FACING);

        mat.translate(0.5f, 0.5f, 0.5f);

        if (face == AttachFace.FLOOR) {
            // do nothing
        } else if (face == AttachFace.CEILING) {
            mat.rotateX((float) Math.toRadians(180.0f));
        } else {
            // wall
            switch (facing) {
                case NORTH -> mat.rotateX((float) Math.toRadians(-90.0f)); // +Y -> -Z
                case SOUTH -> mat.rotateX((float) Math.toRadians(90.0f));  // +Y -> +Z
                case WEST  -> mat.rotateZ((float) Math.toRadians(90.0f));  // +Y -> -X
                case EAST  -> mat.rotateZ((float) Math.toRadians(-90.0f)); // +Y -> +X
            }
        }

        if (face != AttachFace.WALL) {
            float yRot;
            switch (facing) {
                case NORTH -> yRot = 180.0f;
                case SOUTH -> yRot = 0.0f;
                case WEST -> yRot = 90.0f;
                case EAST -> yRot = -90.0f;
                default -> yRot = 0.0f;
            }
            mat.rotateY((float) Math.toRadians(yRot));
        }

        mat.translate(-0.5f, -0.5f, -0.5f);
    }

    // ===== NBT & 网络同步 =====

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        CompoundTag rwl = new CompoundTag();
        rwl.putInt(RWLApi.NBT_R, colorR);
        rwl.putInt(RWLApi.NBT_G, colorG);
        rwl.putInt(RWLApi.NBT_B, colorB);
        rwl.putString(RWLApi.NBT_SOUND, soundId);
        rwl.putLong(RWLApi.NBT_MS_PER_REV, msPerRevolution);

        rwl.putBoolean(NBT_POWERED, powered);
        rwl.putLong(NBT_START_GT, startGameTime);
        rwl.putFloat(NBT_START_ANGLE, startAngleDeg);

        rwl.putFloat(NBT_SPOT_MAX_DIST, spotMaxDist);
        rwl.putFloat(NBT_SPOT_HALF_ANGLE, spotHalfAngleDeg);
        rwl.putFloat(NBT_SPOT_INTENSITY, spotIntensity);
        rwl.putFloat(NBT_SPOT_SOFTNESS, spotSoftness);
        rwl.putFloat(NBT_SPOT_SPEC, spotSpecularStrength);
        rwl.putFloat(NBT_SPOT_SHINE, spotShininess);

        tag.put(RWLApi.NBT_ROOT, rwl);
    }

    @Override
    public void load(CompoundTag tag) {
        boolean oldPowered = this.powered;
        String oldSoundId = this.soundId;

        super.load(tag);

        if (!tag.contains(RWLApi.NBT_ROOT, CompoundTag.TAG_COMPOUND)) return;
        CompoundTag rwl = tag.getCompound(RWLApi.NBT_ROOT);

        this.colorR = rwl.contains(RWLApi.NBT_R) ? RWLApi.clamp255(rwl.getInt(RWLApi.NBT_R)) : this.colorR;
        this.colorG = rwl.contains(RWLApi.NBT_G) ? RWLApi.clamp255(rwl.getInt(RWLApi.NBT_G)) : this.colorG;
        this.colorB = rwl.contains(RWLApi.NBT_B) ? RWLApi.clamp255(rwl.getInt(RWLApi.NBT_B)) : this.colorB;

        this.soundId = rwl.contains(RWLApi.NBT_SOUND) ? rwl.getString(RWLApi.NBT_SOUND) : this.soundId;
        this.msPerRevolution = rwl.contains(RWLApi.NBT_MS_PER_REV) ? RWLApi.clampMs(rwl.getLong(RWLApi.NBT_MS_PER_REV)) : this.msPerRevolution;

        this.powered = rwl.getBoolean(NBT_POWERED);
        this.startGameTime = rwl.getLong(NBT_START_GT);
        this.startAngleDeg = rwl.getFloat(NBT_START_ANGLE);

        if (rwl.contains(NBT_SPOT_MAX_DIST)) this.spotMaxDist = clampF(rwl.getFloat(NBT_SPOT_MAX_DIST), 0.1f, 128.0f);
        if (rwl.contains(NBT_SPOT_HALF_ANGLE)) this.spotHalfAngleDeg = clampF(rwl.getFloat(NBT_SPOT_HALF_ANGLE), 0.1f, 89.0f);
        if (rwl.contains(NBT_SPOT_INTENSITY)) this.spotIntensity = clampF(rwl.getFloat(NBT_SPOT_INTENSITY), 0.0f, 8.0f);
        if (rwl.contains(NBT_SPOT_SOFTNESS)) this.spotSoftness = clampF(rwl.getFloat(NBT_SPOT_SOFTNESS), 0.0f, 1.0f);
        if (rwl.contains(NBT_SPOT_SPEC)) this.spotSpecularStrength = clampF(rwl.getFloat(NBT_SPOT_SPEC), 0.0f, 8.0f);
        if (rwl.contains(NBT_SPOT_SHINE)) this.spotShininess = clampF(rwl.getFloat(NBT_SPOT_SHINE), 1.0f, 512.0f);

        if (this.level != null && this.level.isClientSide()) {
            if (oldPowered != this.powered) {
                org.mirage.gfbs.advanced.rwl.client.RWLClientSoundRegistry.onPowerChange(this, this.powered);
            } else if (this.powered && !safeEq(oldSoundId, this.soundId)) {
                org.mirage.gfbs.advanced.rwl.client.RWLClientSoundRegistry.onRemove(this);
                org.mirage.gfbs.advanced.rwl.client.RWLClientSoundRegistry.onLoad(this);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) this.load(tag);
    }

    // ===== tick：只做配置同步 + 旋转基准维护；不做原版光照 =====

    public static void tick(Level level, BlockPos pos, BlockState state, RotatingWarningLightBlockEntity be) {
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel sl)) return;

        // 每秒同步一次全局配置 / 开关状态（沿用你原来的设计）
        if ((sl.getGameTime() % 20L) == 0L) {
            RWLLevelState st = RWLLevelState.get(sl);
            be.applyFromLevelState(sl, st, true);
        }

        // 这里不再放置 light 方块，也不再做光照传播 —— 完全交给你的自定义着色器引擎。
    }
}
