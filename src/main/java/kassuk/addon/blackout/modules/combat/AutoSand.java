
package kassuk.addon.blackout.modules.combat;

import kassuk.addon.blackout.BlackOut;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;

public class AutoSand extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("target-range")
        .description("The radius in which players get targeted.")
        .defaultValue(4)
        .min(0)
        .sliderMax(5)
        .build()
    );

    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>()
        .name("target-priority")
        .description("How to select the player to target.")
        .defaultValue(SortPriority.LowestHealth)
        .build()
    );

    private final Setting<Integer> height = sgGeneral.add(new IntSetting.Builder()
        .name("height")
        .description("The height to place anvils at.")
        .defaultValue(2)
        .range(0, 5)
        .sliderMax(5)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("The delay in between anvil placements.")
        .defaultValue(10)
        .min(0)
        .sliderMax(50)
        .build()
    );

    private final Setting<Boolean> placeButton = sgGeneral.add(new BoolSetting.Builder()
        .name("place-at-feet")
        .description("Automatically places a button or pressure plate at the targets feet to break the anvils.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> multiPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("multi-place")
        .description("Places multiple anvils at once..")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("Automatically rotates towards the position anvils/pressure plates/buttons are placed.")
        .defaultValue(true)
        .build()
    );

    private PlayerEntity target;
    private int timer;

    public AutoSand() {
        super(BlackOut.COMBATPLUS, "Auto Sand", "chink meta");
    }

    @Override
    public void onActivate() {
        timer = 0;
        target = null;
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (event.screen instanceof AnvilScreen) event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {

        // Check distance + alive
        if (TargetUtils.isBadTarget(target, range.get())) {
            target = TargetUtils.getPlayerTarget(range.get(), priority.get());
            if (TargetUtils.isBadTarget(target, range.get())) return;
        }

        if (placeButton.get()) {
            FindItemResult floorBlock = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof AbstractPressurePlateBlock || Block.getBlockFromItem(itemStack.getItem()) instanceof ButtonBlock);
            BlockUtils.place(target.getBlockPos(), floorBlock, rotate.get(), 0, false);
        }

        if (timer >= delay.get()) {
            timer = 0;

            FindItemResult concrte = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()) instanceof ConcretePowderBlock);
            if (!concrte.found()) return;

            for (int i = height.get(); i > 1; i--) {
                BlockPos blockPos = target.getBlockPos().up().add(0, i, 0);

                for (int j = 0; j < i; j++) {
                    if (!mc.world.getBlockState(target.getBlockPos().up(j + 1)).isReplaceable()) {
                        break;
                    }
                }

                if (BlockUtils.place(blockPos, concrte, rotate.get(), 0) && !multiPlace.get()) break;
            }
        } else {
            timer++;
        }
    }

    @Override
    public String getInfoString() {
        return EntityUtils.getName(target);
    }
}