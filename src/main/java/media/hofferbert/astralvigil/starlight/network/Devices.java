package media.hofferbert.astralvigil.starlight.network;

import java.util.ArrayList;
import java.util.List;
import media.hofferbert.astralvigil.compat.Celestial;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.registry.AVBlockEntities;
import media.hofferbert.astralvigil.starlight.Tint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** The five starlight devices. */
public final class Devices {
    private Devices() {}

    /** Outputs one beam per tint currently held (relays forward whatever they carry). */
    static List<StarlightBlockEntity.Output> relayOutputs(StarlightBlockEntity be, Direction dir) {
        List<StarlightBlockEntity.Output> out = new ArrayList<>(1);
        for (Tint t : Tint.values()) {
            if (be.buffer().get(t) > 0) {
                out.add(new StarlightBlockEntity.Output(dir, t));
            }
        }
        return out;
    }

    /** Gathers starlight under the open sky and beams it out of its front. */
    public static class Collector extends StarlightBlockEntity {
        private double carry;

        public Collector(BlockPos pos, BlockState state) {
            super(AVBlockEntities.COLLECTOR.get(), pos, state, 400);
        }

        @Override
        protected int capacityFromConfig() {
            return AVServerConfig.COLLECTOR_CAPACITY.get();
        }

        @Override
        protected void produce(ServerLevel level) {
            double intensity = Celestial.starlight(level, worldPosition);
            carry += AVServerConfig.COLLECTOR_RATE.get() * intensity;
            int whole = (int) Math.floor(carry);
            if (whole > 0) {
                carry -= whole;
                buffer.insert(Tint.PURE, whole, false);
            }
        }

        /** Current gathering rate per second (for Jade/tooltips). */
        public double currentRate() {
            return level == null ? 0 : AVServerConfig.COLLECTOR_RATE.get() * Celestial.starlight(level, worldPosition);
        }

        @Override
        protected List<Output> outputs() {
            return buffer.get(Tint.PURE) > 0 ? List.of(new Output(facing(), Tint.PURE)) : List.of();
        }

        @Override
        protected boolean accepts(Tint tint, Direction travel) {
            return false;
        }
    }

    /** Relays a beam straight on, losslessly. Only accepts beams entering from its back. */
    public static class Lens extends StarlightBlockEntity {
        public Lens(BlockPos pos, BlockState state) {
            super(AVBlockEntities.LENS.get(), pos, state, 200);
        }

        @Override
        protected int capacityFromConfig() {
            return AVServerConfig.RELAY_CAPACITY.get();
        }

        @Override
        protected List<Output> outputs() {
            return relayOutputs(this, facing());
        }

        @Override
        protected boolean accepts(Tint tint, Direction travel) {
            return travel == facing();
        }
    }

    /** Turns beams from any side towards its front, with a small loss. */
    public static class Mirror extends StarlightBlockEntity {
        public Mirror(BlockPos pos, BlockState state) {
            super(AVBlockEntities.MIRROR.get(), pos, state, 200);
        }

        @Override
        protected int capacityFromConfig() {
            return AVServerConfig.RELAY_CAPACITY.get();
        }

        @Override
        protected double entryLoss() {
            return AVServerConfig.MIRROR_LOSS.get();
        }

        @Override
        protected List<Output> outputs() {
            return relayOutputs(this, facing());
        }

        @Override
        protected boolean accepts(Tint tint, Direction travel) {
            return travel != facing().getOpposite();
        }
    }

    /** Splits pure starlight into Stella (front), Sol (left) and Luna (right). */
    public static class Prism extends StarlightBlockEntity {
        public Prism(BlockPos pos, BlockState state) {
            super(AVBlockEntities.PRISM.get(), pos, state, 200);
        }

        @Override
        protected int capacityFromConfig() {
            return AVServerConfig.RELAY_CAPACITY.get();
        }

        @Override
        protected List<Output> outputs() {
            if (buffer.get(Tint.PURE) <= 0) {
                return List.of();
            }
            Direction f = facing();
            return List.of(
                    new Output(f, Tint.STELLA),
                    new Output(f.getCounterClockWise(), Tint.SOL),
                    new Output(f.getClockWise(), Tint.LUNA));
        }

        private double keep() {
            return 1.0 - AVServerConfig.PRISM_LOSS.get();
        }

        @Override
        protected int sendable(Output output) {
            int share = (int) Math.floor(buffer.get(Tint.PURE) * keep() / 3.0);
            return Math.min(AVServerConfig.BEAM_THROUGHPUT.get(), Math.max(share, buffer.get(Tint.PURE) > 0 ? 1 : 0));
        }

        @Override
        protected void consume(Output output, int sent) {
            buffer.extract(Tint.PURE, (int) Math.ceil(sent / Math.max(keep(), 1e-6)), false);
        }

        @Override
        protected boolean accepts(Tint tint, Direction travel) {
            return tint == Tint.PURE && travel == facing();
        }
    }

    /** Star Anchor: a small starlight receiver used to lay out constellations for rituals. */
    public static class Anchor extends StarlightBlockEntity {
        private long readyAt;

        public Anchor(BlockPos pos, BlockState state) {
            super(AVBlockEntities.STAR_ANCHOR.get(), pos, state, 300);
        }

        @Override
        protected int capacityFromConfig() {
            return Math.max(AVServerConfig.RITUAL_STARLIGHT.get(), 300);
        }

        @Override
        protected List<Output> outputs() {
            return List.of();
        }

        @Override
        protected boolean accepts(Tint tint, Direction travel) {
            return true;
        }

        public long readyAt() {
            return readyAt;
        }

        public void setReadyAt(long value) {
            readyAt = value;
        }

        @Override
        protected void writeExtra(media.hofferbert.astralvigil.compat.DataIO.Writer out) {
            out.putString("ready_at", Long.toString(readyAt));
        }

        @Override
        protected void readExtra(media.hofferbert.astralvigil.compat.DataIO.Reader in) {
            try {
                readyAt = Long.parseLong(in.getString("ready_at", "0"));
            } catch (NumberFormatException e) {
                readyAt = 0;
            }
        }
    }

    /** Stores starlight of every tint. Altars and forges (Phase 4) draw from adjacent reservoirs. */
    public static class Reservoir extends StarlightBlockEntity {
        public Reservoir(BlockPos pos, BlockState state) {
            super(AVBlockEntities.RESERVOIR.get(), pos, state, 20000);
        }

        @Override
        protected int capacityFromConfig() {
            return AVServerConfig.RESERVOIR_CAPACITY.get();
        }

        @Override
        protected List<Output> outputs() {
            return List.of();
        }

        @Override
        protected boolean accepts(Tint tint, Direction travel) {
            return true;
        }

        /**
         * Feeds directly touching devices that accept light travelling away from the reservoir (e.g. a Lens facing
         * away, or an altar/forge). Never feeds other reservoirs or collectors, so there are no loops.
         */
        @Override
        protected void produce(ServerLevel level) {
            int throughput = AVServerConfig.BEAM_THROUGHPUT.get();
            for (Direction d : Direction.values()) {
                if (!(level.getBlockEntity(worldPosition.relative(d)) instanceof StarlightBlockEntity neighbour)
                        || neighbour instanceof Reservoir
                        || neighbour instanceof Collector) {
                    continue;
                }
                for (Tint t : Tint.values()) {
                    int offer = Math.min(throughput, buffer.get(t));
                    if (offer > 0) {
                        buffer.extract(t, neighbour.receive(t, offer, d), false);
                    }
                }
            }
        }
    }
}
