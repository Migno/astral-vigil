package media.hofferbert.astralvigil.registry;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.firmament.Watcher;
import media.hofferbert.astralvigil.firmament.WatcherEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
//? if >=26.1 {
import net.minecraft.resources.ResourceKey;
//?}

public final class AVEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, AstralVigil.MOD_ID);

    /** One entity type per Watcher (own texture/model id), same class. */
    public static final Map<Watcher, Supplier<EntityType<WatcherEntity>>> WATCHERS = watchers();

    public static final Supplier<EntityType<media.hofferbert.astralvigil.eclipse.EclipseEntity>> ECLIPSE = ENTITY_TYPES.register(
            "eclipse", () -> EntityType.Builder.<media.hofferbert.astralvigil.eclipse.EclipseEntity>of(
                            media.hofferbert.astralvigil.eclipse.EclipseEntity::new, MobCategory.MONSTER)
                    .sized(1.6f, 4.0f)
                    .clientTrackingRange(12)
                    .fireImmune()
                    //? if >=26.1 {
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, AstralVigil.id("eclipse"))));
                    //?} else {
                    /*.build("eclipse"));
                    *///?}
    public static final Supplier<EntityType<media.hofferbert.astralvigil.eclipse.EclipseEcho>> ECLIPSE_ECHO = ENTITY_TYPES.register(
            "eclipse_echo", () -> EntityType.Builder.<media.hofferbert.astralvigil.eclipse.EclipseEcho>of(
                            media.hofferbert.astralvigil.eclipse.EclipseEcho::new, MobCategory.MONSTER)
                    .sized(0.7f, 1.5f)
                    .clientTrackingRange(8)
                    .fireImmune()
                    //? if >=26.1 {
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, AstralVigil.id("eclipse_echo"))));
                    //?} else {
                    /*.build("eclipse_echo"));
                    *///?}

    private AVEntities() {}

    private static Map<Watcher, Supplier<EntityType<WatcherEntity>>> watchers() {
        Map<Watcher, Supplier<EntityType<WatcherEntity>>> map = new EnumMap<>(Watcher.class);
        for (Watcher w : Watcher.values()) {
            map.put(w, ENTITY_TYPES.register(w.id(), () -> EntityType.Builder.<WatcherEntity>of(
                            (type, level) -> new WatcherEntity(type, level, w), MobCategory.MISC)
                    .sized(1.2f, 3.0f)
                    .clientTrackingRange(10)
                    .fireImmune()
                    //? if >=26.1 {
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, AstralVigil.id(w.id())))));
                    //?} else {
                    /*.build(w.id())));
                    *///?}
        }
        return map;
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) ->
        {
            WATCHERS.values().forEach(t -> e.put(t.get(), WatcherEntity.createAttributes().build()));
            e.put(ECLIPSE.get(), media.hofferbert.astralvigil.eclipse.EclipseEntity.createAttributes().build());
            e.put(ECLIPSE_ECHO.get(), media.hofferbert.astralvigil.eclipse.EclipseEcho.createAttributes().build());
        });
    }
}
