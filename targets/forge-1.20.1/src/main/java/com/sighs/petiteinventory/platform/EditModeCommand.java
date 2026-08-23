package com.sighs.petiteinventory.platform;

import com.mojang.brigadier.CommandDispatcher;
import com.sighs.petiteinventory.inventory.EditModeService;
import com.sighs.petiteinventory.platform.EditModePayload;
import com.sighs.petiteinventory.platform.NetworkChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/** /petiteinventory edit toggles the inventory canvas editor for the caller. */
public final class EditModeCommand {
    private EditModeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("petiteinventory")
                .then(Commands.literal("edit").executes(context -> toggle(context.getSource()))));
    }

    private static int toggle(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        boolean enabled = EditModeService.toggle(player.getUUID());
        NetworkChannel.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new EditModePayload(enabled));
        source.sendSuccess(() -> Component.literal(enabled ? "Inventory edit mode enabled." : "Inventory edit mode disabled."), false);
        return 1;
    }
}
