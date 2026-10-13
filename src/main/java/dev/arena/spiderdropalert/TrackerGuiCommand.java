package dev.arena.spiderdropalert;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

/** Opens the Slayer drop tracker settings GUI from chat. */
public final class TrackerGuiCommand extends CommandBase {
    private final SpiderDropAlert mod;

    public TrackerGuiCommand(SpiderDropAlert mod) {
        this.mod = mod;
    }

    @Override
    public String getCommandName() {
        return "trackergui";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("tg");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/trackergui (alias: /tg)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        mod.requestTrackerGui();
    }
}
