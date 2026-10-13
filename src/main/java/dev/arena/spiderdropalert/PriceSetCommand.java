package dev.arena.spiderdropalert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;

/** Client command for setting the manual price of a held or named item. */
public final class PriceSetCommand extends CommandBase {
    private final SpiderDropAlert mod;
    private final String commandName;

    public PriceSetCommand(SpiderDropAlert mod) {
        this(mod, "priceset");
    }

    PriceSetCommand(SpiderDropAlert mod, String commandName) {
        this.mod = mod;
        this.commandName = commandName;
    }

    @Override
    public String getCommandName() {
        return commandName;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        String command = "/" + commandName;
        return command + " <price> (held item; e.g. 110k) or "
                + command + " <item name> <price>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private String getPriceExample() {
        String command = "/" + commandName;
        return command + " <price>, e.g. " + command + " 110k";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            String heldItem = mod.getHeldItemName();
            if (heldItem.length() == 0) {
                sender.addChatMessage(new ChatComponentText(
                        "No held item detected. Hold an item, then use " + getPriceExample() + "."));
            } else {
                sender.addChatMessage(new ChatComponentText("Detected held item: " + heldItem
                        + ". Set its manual price with " + getPriceExample() + "."));
            }
            return;
        }

        String item;
        String priceArg;
        if (args.length == 1) {
            item = mod.getHeldItemName();
            priceArg = args[0];
            if (item.length() == 0) {
                sender.addChatMessage(new ChatComponentText(
                        "Hold the item you want to price, then run " + getPriceExample() + "."));
                return;
            }
        } else {
            StringBuilder itemName = new StringBuilder();
            for (int i = 0; i < args.length - 1; i++) {
                if (i > 0) {
                    itemName.append(' ');
                }
                itemName.append(args[i]);
            }
            item = itemName.toString().trim();
            if (item.length() >= 2 && item.startsWith("\"") && item.endsWith("\"")) {
                item = item.substring(1, item.length() - 1).trim();
            }
            priceArg = args[args.length - 1];
        }

        long price;
        try {
            price = parsePrice(priceArg);
        } catch (NumberFormatException | ArithmeticException exception) {
            sender.addChatMessage(new ChatComponentText("Invalid price. Use a coin amount such as 110000, 110k, 1.5m, or 1b."));
            return;
        }
        if (price < 0L) {
            sender.addChatMessage(new ChatComponentText("Price cannot be negative."));
            return;
        }
        if (item.length() == 0) {
            sender.addChatMessage(new ChatComponentText("Enter an item name. Usage: "
                    + getCommandUsage(sender)));
            return;
        }

        mod.setItemPrice(item, price);
        sender.addChatMessage(new ChatComponentText("Saved price for " + item + ": "
                + SpiderDropAlert.formatPrice(price) + " coins."));
    }

    static long parsePrice(String value) {
        if (value == null) {
            throw new NumberFormatException("Missing price");
        }
        String amount = value.trim().toLowerCase(Locale.ROOT);
        if (amount.length() == 0) {
            throw new NumberFormatException("Missing price");
        }

        BigDecimal multiplier = BigDecimal.ONE;
        char last = amount.charAt(amount.length() - 1);
        boolean hasSuffix = true;
        if (last == 'k') {
            multiplier = BigDecimal.valueOf(1000L);
            amount = amount.substring(0, amount.length() - 1);
        } else if (last == 'm') {
            multiplier = BigDecimal.valueOf(1000000L);
            amount = amount.substring(0, amount.length() - 1);
        } else if (last == 'b') {
            multiplier = BigDecimal.valueOf(1000000000L);
            amount = amount.substring(0, amount.length() - 1);
        } else {
            hasSuffix = false;
        }

        int comma = amount.indexOf(',');
        if (hasSuffix && comma >= 0 && comma == amount.lastIndexOf(',')) {
            int decimalDigits = amount.length() - comma - 1;
            if (decimalDigits > 0 && decimalDigits <= 2) {
                amount = amount.substring(0, comma) + "." + amount.substring(comma + 1);
            } else {
                amount = amount.replace(",", "");
            }
        } else {
            amount = amount.replace(",", "");
        }

        BigDecimal coins = new BigDecimal(amount).multiply(multiplier)
                .setScale(0, RoundingMode.HALF_UP);
        return coins.longValueExact();
    }
}
