package dev.arena.spiderdropalert;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

/** Opens the quick note pad from chat with /qn. */
public final class QuickNoteCommand extends CommandBase {
    private final SpiderDropAlert mod;

    public QuickNoteCommand(SpiderDropAlert mod) {
        this.mod = mod;
    }

    @Override
    public String getCommandName() {
        return "qn";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("quicknote");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/qn (alias: /quicknote)";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        mod.requestQuickNote();
    }
}
