package media.hofferbert.astralvigil.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import media.hofferbert.astralvigil.AstralVigil;
import media.hofferbert.astralvigil.config.AVServerConfig;
import media.hofferbert.astralvigil.gear.GearData;
import media.hofferbert.astralvigil.gear.GearItems;
import media.hofferbert.astralvigil.gear.GearKind;
import media.hofferbert.astralvigil.gear.GearProgress;
import media.hofferbert.astralvigil.gear.Sigil;
import media.hofferbert.astralvigil.network.AVNetwork;
import media.hofferbert.astralvigil.registry.AVComponents;
import media.hofferbert.astralvigil.registry.AVItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;
//? if >=26.1 {
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
//?} else {
/*import net.neoforged.neoforge.network.PacketDistributor;
*///?}

/** Ability/stance key bindings and living-gear tooltips. */
public final class GearClient {
    //? if >=26.1 {
    static final KeyMapping.Category CATEGORY = new KeyMapping.Category(AstralVigil.id("astralvigil"));
    static final KeyMapping ABILITY =
            new KeyMapping("key.astralvigil.ability", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    static final KeyMapping STANCE =
            new KeyMapping("key.astralvigil.stance", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    //?} else {
    /*static final String CATEGORY = "key.categories.astralvigil";
    static final KeyMapping ABILITY =
            new KeyMapping("key.astralvigil.ability", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    static final KeyMapping STANCE =
            new KeyMapping("key.astralvigil.stance", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    *///?}

    private GearClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(GearClient::onRegisterKeys);
        //? if <26.1 {
        /*modBus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(GearClient::itemProperties));
        *///?}
        NeoForge.EVENT_BUS.addListener(GearClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(GearClient::onTooltip);
    }

    //? if <26.1 {
    /*// 1.21.1 drives bow/crossbow frames through item properties; reuse vanilla's functions for our weapons.
    private static void itemProperties() {
        for (String id : new String[] {"pull", "pulling"}) {
            var rl = net.minecraft.resources.ResourceLocation.withDefaultNamespace(id);
            net.minecraft.client.renderer.item.ItemProperties.register(AVItems.CRESCENT.get(), rl,
                    net.minecraft.client.renderer.item.ItemProperties.getProperty(new ItemStack(net.minecraft.world.item.Items.BOW), rl));
        }
        for (String id : new String[] {"pull", "pulling", "charged", "firework"}) {
            var rl = net.minecraft.resources.ResourceLocation.withDefaultNamespace(id);
            net.minecraft.client.renderer.item.ItemProperties.register(AVItems.TIDECALLER.get(), rl,
                    net.minecraft.client.renderer.item.ItemProperties.getProperty(new ItemStack(net.minecraft.world.item.Items.CROSSBOW), rl));
        }
        net.minecraft.client.renderer.item.ItemProperties.register(AVItems.STARLIGHT_AEGIS.get(),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0f : 0.0f);
    }
    *///?}

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        //? if >=26.1 {
        event.registerCategory(CATEGORY);
        //?}
        event.register(ABILITY);
        event.register(STANCE);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        while (ABILITY.consumeClick()) {
            send(AVNetwork.GearAction.USE_ABILITY);
        }
        while (STANCE.consumeClick()) {
            send(AVNetwork.GearAction.CYCLE_STANCE);
        }
    }

    private static void send(int action) {
        //? if >=26.1 {
        ClientPacketDistributor.sendToServer(new AVNetwork.GearAction(action));
        //?} else {
        /*PacketDistributor.sendToServer(new AVNetwork.GearAction(action));
        *///?}
    }

    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tip = event.getToolTip();
        if (stack.getItem() instanceof GearItems.SigilItem sigilItem) {
            Sigil s = sigilItem.sigil();
            tip.add(1, Component.translatable(s.translationKey() + ".desc").withStyle(ChatFormatting.GRAY));
            tip.add(2, Component.translatable("gear.astralvigil.sigil_cost", s.baseCost(), s.baseCooldown() / 20)
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        GearKind kind = GearItems.kind(stack).orElse(null);
        if (kind == null) {
            return;
        }
        GearData d = stack.get(AVComponents.GEAR.get());
        if (d == null) {
            d = GearData.EMPTY;
        }
        int line = 1;
        tip.add(line++, Component.translatable("gear.astralvigil.level." + d.level()).withStyle(ChatFormatting.GOLD));
        int toNext = GearProgress.toNext(d.deeds());
        if (toNext > 0) {
            tip.add(line++, Component.translatable(kind.deedKey(), toNext).withStyle(ChatFormatting.GRAY));
        }
        int cap = AVServerConfig.SPEC.isLoaded() ? AVServerConfig.GEAR_CHARGE_CAPACITY.get() : 200;
        tip.add(line++, Component.translatable("gear.astralvigil.charge", d.charge(), cap).withStyle(ChatFormatting.AQUA));
        int slots = GearProgress.slots(d.level());
        for (int i = 0; i < slots; i++) {
            if (i < d.sigils().size()) {
                Sigil s = Sigil.byId(d.sigils().get(i)).orElse(null);
                boolean active = i == Math.floorMod(d.stance(), d.sigils().size());
                Component name = s == null ? Component.literal("?") : Component.translatable(s.translationKey());
                tip.add(line++, Component.literal(active ? " ◆ " : " ◇ ")
                        .append(name)
                        .withStyle(active ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.DARK_PURPLE));
            } else {
                tip.add(line++, Component.translatable("gear.astralvigil.empty_slot").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        tip.add(line, Component.translatable("gear.astralvigil.hint",
                        ABILITY.getTranslatedKeyMessage(), STANCE.getTranslatedKeyMessage())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
