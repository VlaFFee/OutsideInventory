package com.example.hubui.network;

import com.example.hubui.HubMenu;
import com.example.hubui.HubRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент -> сервер: создать предмет по рецепту. Сервер сам проверяет материалы. */
public record CraftPacket(ResourceLocation recipe) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeResourceLocation(recipe);
    }

    public static CraftPacket decode(FriendlyByteBuf buf) {
        return new CraftPacket(buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            var p = c.getSender();
            if (p == null || !(p.containerMenu instanceof HubMenu m)) return;
            p.server.getRecipeManager().byKey(recipe).ifPresent(r -> {
                if (r instanceof HubRecipe hr) m.craft(p, hr);
            });
        });
        c.setPacketHandled(true);
    }
}
