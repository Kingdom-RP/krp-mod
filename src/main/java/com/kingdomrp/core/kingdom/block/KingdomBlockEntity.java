package com.kingdomrp.core.kingdom.block;

import com.kingdomrp.core.registry.KRPBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * BE блока королевства: слот для хартии + id активного королевства
 * ({@code null} = блок поставлен, но королевство ещё не создано).
 */
public class KingdomBlockEntity extends BlockEntity implements MenuProvider {

    public static final int RESOURCE_MAX_STACK = 512;

    private final SimpleContainer charterSlot = new SimpleContainer(1);
    // Слоты-буферы ресурсов: 0=еда, 1=материалы, 2=довольствие. До 512 предметов.
    private final SimpleContainer resources = new SimpleContainer(3) {
        @Override public int getMaxStackSize() { return RESOURCE_MAX_STACK; }
    };
    @Nullable private UUID kingdomId;
    private int beamColor = 0xFFFFFF;    // цвет луча (клиент); сервер шлёт из цвета королевства

    private boolean absorbing = false;   // защита от рекурсии листенера при поглощении

    public KingdomBlockEntity(BlockPos pos, BlockState state) {
        super(KRPBlockEntities.KINGDOM_BLOCK.get(), pos, state);
        charterSlot.addListener(c -> setChanged());
        resources.addListener(c -> { setChanged(); onResourcesChanged(); });
    }

    /** Депозит в слот → сразу поглотить в характеристику (событийно, без тикера). */
    private void onResourcesChanged() {
        if (absorbing || !(level instanceof net.minecraft.server.level.ServerLevel sl) || !isActive()) return;
        var k = com.kingdomrp.core.kingdom.KingdomData.get(sl.getServer()).byId(kingdomId);
        if (k == null) return;
        absorbing = true;
        try {
            com.kingdomrp.core.kingdom.upkeep.KingdomUpkeep.absorb(this, k);
        } finally {
            absorbing = false;
        }
        com.kingdomrp.core.kingdom.upkeep.KingdomBuffs.update(sl.getServer(), k);  // депозит → пересчёт баффов
        com.kingdomrp.core.kingdom.KingdomData.get(sl.getServer()).markDirty();
        com.kingdomrp.core.kingdom.KingdomSync.broadcast(sl.getServer(), k);
    }

    public SimpleContainer getCharterSlot() { return charterSlot; }
    public ItemStack getCharter()           { return charterSlot.getItem(0); }
    public SimpleContainer getResources()   { return resources; }

    @Nullable public UUID getKingdomId()    { return kingdomId; }
    public boolean isActive()               { return kingdomId != null; }
    public int getBeamColor()               { return beamColor; }

    /** Форс-пуш обновления BE на клиент (пересинк цвета луча при смене цвета королевства). */
    public void syncToClient() {
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void setKingdomId(@Nullable UUID id) {
        this.kingdomId = id;
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);  // синк на клиент
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        charterSlot.setItem(0, tag.contains("charter")
                ? ItemStack.parseOptional(registries, tag.getCompound("charter"))
                : ItemStack.EMPTY);
        for (int i = 0; i < 3; i++)
            resources.setItem(i, tag.contains("res" + i)
                    ? ItemStack.parseOptional(registries, tag.getCompound("res" + i))
                    : ItemStack.EMPTY);
        kingdomId = tag.hasUUID("kingdom") ? tag.getUUID("kingdom") : null;
        if (tag.contains("beamColor")) beamColor = tag.getInt("beamColor");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ItemStack charter = getCharter();
        if (!charter.isEmpty()) tag.put("charter", charter.save(registries));
        for (int i = 0; i < 3; i++) {
            ItemStack s = resources.getItem(i);
            if (!s.isEmpty()) tag.put("res" + i, s.save(registries));
        }
        if (kingdomId != null)  tag.putUUID("kingdom", kingdomId);
    }

    // Синхронизация на клиент (нужна kingdomId для активного меню по ПКМ).

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        if (kingdomId != null) {
            tag.putUUID("kingdom", kingdomId);
            // Цвет луча берём из данных королевства (сервер), чтобы клиент рисовал маяк.
            if (level != null && level.getServer() != null) {
                var k = com.kingdomrp.core.kingdom.KingdomData.get(level.getServer()).byId(kingdomId);
                if (k != null) tag.putInt("beamColor", k.getColor());
            }
        }
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.kingdomrpcore.kingdom_block");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new KingdomMenu(id, inv, this, isKing(player));
    }

    /** Является ли игрок королём этого королевства (для гейта изъятия ресурсов). */
    public boolean isKing(Player player) {
        if (kingdomId == null || level == null || level.getServer() == null) return false;
        var k = com.kingdomrp.core.kingdom.KingdomData.get(level.getServer()).byId(kingdomId);
        return k != null && k.isKing(player.getUUID());
    }
}
