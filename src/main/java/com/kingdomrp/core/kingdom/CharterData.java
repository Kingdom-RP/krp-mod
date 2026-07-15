package com.kingdomrp.core.kingdom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Данные подписанной хартии (data component {@code kingdomrpcore:charter}).
 * Неподписанная хартия компонента не имеет.
 *
 * @param name      название будущего королевства
 * @param king      подписавший первым — будущий король
 * @param cosigners игроки, поддержавшие создание (нужно ≥2 для активации блока)
 */
public record CharterData(String name, UUID king, List<UUID> cosigners) {

    public static final Codec<CharterData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(CharterData::name),
            UUIDUtil.CODEC.fieldOf("king").forGetter(CharterData::king),
            UUIDUtil.CODEC.listOf().fieldOf("cosigners").forGetter(CharterData::cosigners)
    ).apply(i, CharterData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CharterData> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, CharterData::name,
                    UUIDUtil.STREAM_CODEC, CharterData::king,
                    UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), CharterData::cosigners,
                    CharterData::new);

    public static CharterData sign(String name, UUID king) {
        return new CharterData(name, king, new ArrayList<>());
    }

    public boolean hasCosigner(UUID p) { return cosigners.contains(p); }

    /** Копия с добавленным соподписантом (без дублей, не сам король). */
    public CharterData withCosigner(UUID p) {
        if (p.equals(king) || cosigners.contains(p)) return this;
        List<UUID> next = new ArrayList<>(cosigners);
        next.add(p);
        return new CharterData(name, king, next);
    }

    /** Достаточно подписей для создания королевства (король + ≥2 соподписанта). */
    public boolean readyToCreate() { return cosigners.size() >= 2; }
}
