package com.jvn.lodged.item;

import com.jvn.lodged.Lodged;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LodgedItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Lodged.MOD_ID);

    public static final DeferredItem<BandageItem> BANDAGE =
            ITEMS.registerItem("bandage", BandageItem::new, new Item.Properties().stacksTo(16));

    private LodgedItems() {
    }

    public static void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(BANDAGE);
        }
    }
}
