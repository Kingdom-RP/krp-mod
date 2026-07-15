package com.kingdomrp.core.kingdom.block;

import com.kingdomrp.core.kingdom.CharterData;
import com.kingdomrp.core.kingdom.item.CharterItem;
import com.kingdomrp.core.kingdom.upkeep.Characteristic;
import com.kingdomrp.core.registry.KRPMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

/**
 * Меню блока королевства: слот хартии + (для активного) 3 слота-буфера ресурсов
 * содержания (еда / материалы / довольствие, до 512 предметов). Ресурсы поглощаются
 * тикером блока в характеристики.
 */
public class KingdomMenu extends AbstractContainerMenu {

    // Позиции слотов (используются и экраном). Активная раскладка — вертикальные ряды.
    public static final int CHARTER_ACTIVE_X = 8, CHARTER_ACTIVE_Y = 18;
    public static final int CHARTER_CREATE_X = 80, CHARTER_CREATE_Y = 18;
    public static final int RES_SLOT_X = 8;
    public static final int[] RES_SLOT_Y = {40, 66, 92};
    public static final int INV_Y_ACTIVE = 122, HOTBAR_Y_ACTIVE = 180;
    public static final int INV_Y_CREATE = 84, HOTBAR_Y_CREATE = 142;

    private final Container charterSlot;
    private final Container resources;
    @Nullable private final KingdomBlockEntity be;
    private final BlockPos pos;
    private final boolean active;
    private final boolean isKing;
    private final int contentSlots;   // сколько не-инвентарных слотов

    /** Серверный конструктор. isKing вычисляет BE (есть доступ к KingdomData). */
    public KingdomMenu(int id, Inventory playerInv, KingdomBlockEntity be, boolean isKing) {
        this(id, playerInv, be.getCharterSlot(), be.getResources(), be, be.getBlockPos(), be.isActive(), isKing);
    }

    /** Клиентский конструктор (из сети): pos + isKing. */
    public KingdomMenu(int id, Inventory playerInv, RegistryFriendlyByteBuf buf) {
        this(id, playerInv, buf.readBlockPos(), buf.readBoolean());
    }

    private KingdomMenu(int id, Inventory playerInv, BlockPos pos, boolean isKing) {
        this(id, playerInv, resolveCharter(playerInv, pos), resolveResources(playerInv, pos),
                null, pos, resolveActive(playerInv, pos), isKing);
    }

    private KingdomMenu(int id, Inventory playerInv, Container charterSlot, Container resources,
                        @Nullable KingdomBlockEntity be, BlockPos pos, boolean active, boolean isKing) {
        super(KRPMenus.KINGDOM_BLOCK.get(), id);
        this.charterSlot = charterSlot;
        this.resources = resources;
        this.be = be;
        this.pos = pos;
        this.active = active;
        this.isKing = isKing;

        int charterX = active ? CHARTER_ACTIVE_X : CHARTER_CREATE_X;
        int charterY = active ? CHARTER_ACTIVE_Y : CHARTER_CREATE_Y;
        this.addSlot(new Slot(charterSlot, 0, charterX, charterY) {
            @Override public boolean mayPlace(ItemStack stack) {
                if (active) return false;
                if (!(stack.getItem() instanceof CharterItem)) return false;
                CharterData d = CharterItem.data(stack);
                return d != null && d.king().equals(playerInv.player.getUUID());
            }
            @Override public boolean mayPickup(Player player) { return !active; }
            @Override public int getMaxStackSize() { return 1; }
        });

        if (active) {
            for (int i = 0; i < 3; i++) {
                final Characteristic c = Characteristic.VALUES[i];
                this.addSlot(new Slot(resources, i, RES_SLOT_X, RES_SLOT_Y[i]) {
                    @Override public boolean mayPlace(ItemStack stack) { return c.accepts(stack); }
                    @Override public boolean mayPickup(Player player) { return isKing; }  // забрать — только король
                    @Override public int getMaxStackSize() { return KingdomBlockEntity.RESOURCE_MAX_STACK; }
                    @Override public int getMaxStackSize(ItemStack stack) { return KingdomBlockEntity.RESOURCE_MAX_STACK; }
                });
            }
        }
        this.contentSlots = active ? 4 : 1;

        int invY = active ? INV_Y_ACTIVE : INV_Y_CREATE;
        int hotbarY = active ? HOTBAR_Y_ACTIVE : HOTBAR_Y_CREATE;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, invY + row * 18));
        for (int col = 0; col < 9; col++)
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, hotbarY));
    }

    private static Container resolveCharter(Inventory inv, BlockPos pos) {
        return inv.player.level().getBlockEntity(pos) instanceof KingdomBlockEntity kbe
                ? kbe.getCharterSlot() : new SimpleContainer(1);
    }

    private static Container resolveResources(Inventory inv, BlockPos pos) {
        return inv.player.level().getBlockEntity(pos) instanceof KingdomBlockEntity kbe
                ? kbe.getResources() : new SimpleContainer(3);
    }

    private static boolean resolveActive(Inventory inv, BlockPos pos) {
        return inv.player.level().getBlockEntity(pos) instanceof KingdomBlockEntity kbe && kbe.isActive();
    }

    @Nullable public KingdomBlockEntity getBlockEntity() { return be; }
    public BlockPos getPos() { return pos; }
    public boolean isActive() { return active; }
    public ItemStack getCharter() { return charterSlot.getItem(0); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();
        int invEnd = this.slots.size();
        if (index < contentSlots) {                        // из контента → в инвентарь
            if (!this.moveItemStackTo(stack, contentSlots, invEnd, true)) return ItemStack.EMPTY;
        } else {                                           // из инвентаря → в контент (по mayPlace)
            if (!this.moveItemStackTo(stack, 0, contentSlots, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) { return charterSlot.stillValid(player); }
}
