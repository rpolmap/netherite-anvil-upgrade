package com.example.netheriteanvil.mixin;

import com.example.netheriteanvil.AnvilUpgradeLogic;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Shadow @Final private DataSlot cost;
    @Shadow private int repairItemCountCost;
    @Shadow private String itemName;
    @Shadow @Final protected Player player;

    @Inject(method = "createResult", at = @At("HEAD"), cancellable = true)
    private void netheriteUpgrade$createResult(CallbackInfo ci) {
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        // Not our recipe -> let vanilla handle everything.
        if (!AnvilUpgradeLogic.isUpgradePair(left, right)) {
            return;
        }

        AnvilUpgradeLogic.Result result = AnvilUpgradeLogic.combine(
                left, right, this.itemName, this.player.hasInfiniteMaterials());

        if (result == null) {
            menu.getSlot(2).set(ItemStack.EMPTY);
            this.cost.set(0);
        } else {
            menu.getSlot(2).set(result.stack());
            this.cost.set(result.cost());
        }
        this.repairItemCountCost = 0; // right slot is consumed whole on take
        menu.broadcastChanges();
        ci.cancel();
    }
}
