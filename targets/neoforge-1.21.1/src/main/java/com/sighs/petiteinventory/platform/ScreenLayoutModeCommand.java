package com.sighs.petiteinventory.platform;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Commands that switch the default instead of materializing a list of screens. */
public final class ScreenLayoutModeCommand {
    private ScreenLayoutModeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("petiteinventory")
                .then(Commands.literal("blacklist").executes(context -> setDefault(context.getSource(), true)))
                .then(Commands.literal("whitelist").executes(context -> setDefault(context.getSource(), false))));
    }

    private static int setDefault(CommandSourceStack source, boolean defaultEnabled) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }
        PacketDistributor.sendToPlayer(player, new ScreenLayoutModePayload(defaultEnabled));
        source.sendSuccess(() -> Component.literal(defaultEnabled
                ? "Screen layout blacklist mode enabled."
                : "Screen layout whitelist mode enabled."), false);
        return 1;
    }
}
