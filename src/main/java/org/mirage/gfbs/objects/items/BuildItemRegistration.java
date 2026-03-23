package org.mirage.gfbs.objects.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import org.mirage.gfbs.objects.blocks.BlockRegistration;

import static org.mirage.gfbs.MirageGFBS.ITEMS;

public class BuildItemRegistration {

    public static void init() {
    }

    // 建筑方块
    public static final RegistryObject<Item> QS_WALL_ITEM =
            ITEMS.register("qs_wall",
                    () -> new BlockItem(BlockRegistration.QS_WALL.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_BLUE_ITEM =
            ITEMS.register("brickwall_blue",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_BLUE.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_GRAY_ITEM =
            ITEMS.register("brickwall_gray",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_GRAY.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_OLIVEBROWN_ITEM =
            ITEMS.register("brickwall_olivebrown",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_OLIVEBROWN.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_P_RED_ITEM =
            ITEMS.register("brickwall_p_red",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_P_RED.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_Q_BLUE_ITEM =
            ITEMS.register("brickwall_q_blue",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_Q_BLUE.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_Q_OLIVEBROWN_ITEM =
            ITEMS.register("brickwall_q_olivebrown",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_Q_OLIVEBROWN.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_S_GRAY_ITEM =
            ITEMS.register("brickwall_s_gray",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_S_GRAY.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_SP_ED_ITEM =
            ITEMS.register("brickwall_sp_ed",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_SP_ED.get(), new Item.Properties()));

    public static final RegistryObject<Item> BRICKWALL_WHITE_ITEM =
            ITEMS.register("brickwall_white",
                    () -> new BlockItem(BlockRegistration.BRICKWALL_WHITE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLOOR_BLACK_ITEM =
            ITEMS.register("floor_black",
                    () -> new BlockItem(BlockRegistration.FLOOR_BLACK.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLOOR_WHITE_ITEM =
            ITEMS.register("floor_white",
                    () -> new BlockItem(BlockRegistration.FLOOR_WHITE.get(), new Item.Properties()));

    public static final RegistryObject<Item> FLOOR_OLIVEBROWN_ITEM =
            ITEMS.register("floor_olivebrown",
                    () -> new BlockItem(BlockRegistration.FLOOR_OLIVEBROWN.get(), new Item.Properties()));

}
