package dev.arena.spiderdropalert;

import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/** Opens and controls the tracker settings screen. */
public final class SpiderTrackerCommand extends CommandBase {
    private final SpiderDropAlert mod;

    public SpiderTrackerCommand(SpiderDropAlert mod) {
        this.mod = mod;
    }

    @Override
    public String getCommandName() {
        return "spidertracker";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/spidertracker [gui|toggle|clear]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String action = args.length == 0 ? "gui" : args[0].toLowerCase(Locale.ROOT);
        if ("gui".equals(action)) {
            mod.requestTrackerGui();
        } else if ("toggle".equals(action)) {
            mod.toggleTracker();
            sender.addChatMessage(new ChatComponentText("Slayer drop tracker is "
                    + (mod.isTrackerEnabled() ? "ON" : "OFF") + "."));
        } else if ("clear".equals(action)) {
            mod.clearHistory();
            sender.addChatMessage(new ChatComponentText("All Slayer and Fishing tracker histories cleared."));
        } else {
            sender.addChatMessage(new ChatComponentText("Usage: " + getCommandUsage(sender)));
        }
    }
}
