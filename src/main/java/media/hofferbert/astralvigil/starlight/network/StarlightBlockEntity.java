package media.hofferbert.astralvigil.starlight.network;

import java.util.ArrayList;
import java.util.List;
import media.hofferbert.astralvigil.compat.DataIO;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.starlight.StarlightBuffer;
import media.hofferbert.astralvigil.starlight.Tint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
//? if >=26.1 {
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
//?}

/**
 * Base for every starlight device. Holds a tinted {@link StarlightBuffer}, emits beams through {@link #outputs()} and
 * receives beams via {@link #receive}.
 *
 * <p>Performance: devices only work once per second (every 20 ticks): produce, re-trace their beams (at most
 * {@code beamRange} block lookups each) and push starlight to the receiver they hit. No global scans; clients only
 * get an update when beams or stored amounts change.
 */
public abstract class StarlightBlockEntity extends BlockEntity {
    public static final int WORK_INTERVAL = 20;

    /** One emitted beam as seen by the renderer. Length in tenths of a block. */
    public record Beam(Direction direction, Tint tint, int lengthTenths) {}

    /** A beam output slot of this device. */
    public record Output(Direction direction, Tint tint) {}

    protected final StarlightBuffer buffer;
    private List<Beam> beams = List.of();
    private int ticks;
    private int lastSyncedTotal = -1;

    protected StarlightBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity) {
        super(type, pos, state);
        this.buffer = new StarlightBuffer(capacity);
    }

    // ------------------------------------------------------------------ behaviour hooks

    /** Beams this device emits right now (empty for pure receivers). */
    protected abstract List<Output> outputs();

    /** Whether a beam of {@code tint} travelling in {@code travel} may enter this device. */
    protected abstract boolean accepts(Tint tint, Direction travel);

    /** Fraction of incoming starlight lost on entry. */
    protected double entryLoss() {
        return 0.0;
    }

    /** Called once per work interval before beams are traced (collectors gather here). */
    protected void produce(ServerLevel level) {}

    /** How much starlight {@code output} may send this interval. */
    protected int sendable(Output output) {
        return Math.min(AVServerConfig.BEAM_THROUGHPUT.get(), buffer.get(output.tint()));
    }

    /** Removes what was actually sent through {@code output}. */
    protected void consume(Output output, int sent) {
        buffer.extract(output.tint(), sent, false);
    }

    protected int capacityFromConfig() {
        return buffer.capacity();
    }

    // ------------------------------------------------------------------ API

    public StarlightBuffer buffer() {
        return buffer;
    }

    public List<Beam> beams() {
        return beams;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Offers starlight carried by a beam travelling in {@code travel}.
     *
     * @return amount taken from the sender (before entry loss)
     */
    public int receive(Tint tint, int amount, Direction travel) {
        if (amount <= 0 || !accepts(tint, travel)) {
            return 0;
        }
        double keep = 1.0 - entryLoss();
        int space = buffer.space();
        int taken = (int) Math.min(amount, Math.floor(space / Math.max(keep, 1e-6)));
        int stored = (int) Math.floor(taken * keep);
        buffer.insert(tint, stored, false);
        if (taken > 0) {
            setChanged();
        }
        return taken;
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, StarlightBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) {
            be.tickServer(serverLevel);
        }
    }

    void tickServer(ServerLevel level) {
        if (++ticks % WORK_INTERVAL != 0) {
            return;
        }
        buffer.setCapacity(capacityFromConfig());
        produce(level);

        int range = AVServerConfig.BEAM_RANGE.get();
        List<Beam> traced = new ArrayList<>();
        for (Output output : outputs()) {
            BeamTracer.Hit hit = BeamTracer.trace(level, worldPosition, output.direction(), range);
            traced.add(new Beam(output.direction(), output.tint(), hit.lengthTenths()));
            if (hit.receiver() != null && hit.receiver() != this) {
                int offer = sendable(output);
                if (offer > 0) {
                    int taken = hit.receiver().receive(output.tint(), offer, output.direction());
                    if (taken > 0) {
                        consume(output, taken);
                        setChanged();
                    }
                }
            }
        }

        boolean beamsChanged = !traced.equals(beams);
        beams = List.copyOf(traced);
        int total = buffer.total();
        if (beamsChanged || total != lastSyncedTotal) {
            lastSyncedTotal = total;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    // ------------------------------------------------------------------ persistence + sync

    /** Subclass state (items, progress …), saved with the block entity and synced to clients. */
    protected void writeExtra(DataIO.Writer out) {}

    protected void readExtra(DataIO.Reader in) {}

    /** Pushes the current state to clients (items, progress) outside the once-per-second work cycle. */
    public void syncNow() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private int[] beamArray() {
        int[] out = new int[beams.size() * 3];
        for (int i = 0; i < beams.size(); i++) {
            Beam b = beams.get(i);
            out[i * 3] = b.direction().get3DDataValue();
            out[i * 3 + 1] = b.tint().ordinal();
            out[i * 3 + 2] = b.lengthTenths();
        }
        return out;
    }

    private void readBeams(int[] data) {
        List<Beam> list = new ArrayList<>();
        for (int i = 0; i + 2 < data.length; i += 3) {
            list.add(new Beam(Direction.from3DDataValue(data[i]), Tint.byOrdinal(data[i + 1]), data[i + 2]));
        }
        beams = List.copyOf(list);
    }

    //? if >=26.1 {
    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putIntArray("starlight", buffer.toArray());
        output.putIntArray("beams", beamArray());
        writeExtra(DataIO.writer(output));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntArray("starlight").ifPresent(buffer::fromArray);
        input.getIntArray("beams").ifPresent(this::readBeams);
        readExtra(DataIO.reader(input));
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("starlight", buffer.toArray());
        tag.putIntArray("beams", beamArray());
        writeExtra(DataIO.writer(tag, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.fromArray(tag.getIntArray("starlight"));
        readBeams(tag.getIntArray("beams"));
        readExtra(DataIO.reader(tag, registries));
    }
    *///?}

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //? if >=26.1 {
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            media.hofferbert.astralvigil.block.RitualBlock.dropContents(level, pos, this);
        }
    }
    //?}
}
