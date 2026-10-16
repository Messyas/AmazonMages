package com.example.amazonMages.dev;

import java.util.Locale;

/** Android-free command parser, easy to unit test. */
public final class DevConsole {
    public interface Commands {
        String position();
        String debug(String mode);
        String teleport(double x, double y);
        String god(boolean enabled);
        String heal();
        String give(String item);
        String use(String item);
        String inventory();
        String spawn(String species, int count);
        String clearEnemies();
        String level(int amount);
    }
    private final Commands commands;
    public DevConsole(Commands commands) { this.commands = commands; }

    public String execute(String input) {
        if (input == null || input.trim().isEmpty()) return "Digite help.";
        String[] parts = input.trim().toLowerCase(Locale.ROOT).split("\\s+");
        try {
            switch (parts[0]) {
                case "help": return "debug on/off/info | help | pos | locais | tp <nome> | tp <x> <y> | god on/off | heal | give <id> | use <id> | inventory | spawn <id> [1-10] | clear | level <1-99>";
                case "debug":
                    if (parts.length == 2 && (parts[1].equals("on") ||
                        parts[1].equals("off") || parts[1].equals("info")))
                        return commands.debug(parts[1]);
                    break;
                case "pos": return commands.position();
                case "locais": return DevLocations.names();
                case "tp":
                    if (parts.length == 2) {
                        double[] location = DevLocations.find(parts[1]);
                        return location == null ? "Local desconhecido. Use locais." : commands.teleport(location[0], location[1]);
                    }
                    if (parts.length == 3) return commands.teleport(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
                    break;
                case "god":
                    if (parts.length == 2 && (parts[1].equals("on") || parts[1].equals("off")))
                        return commands.god(parts[1].equals("on"));
                    break;
                case "heal": if (parts.length == 1) return commands.heal(); break;
                case "give": if (parts.length == 2) return commands.give(parts[1]); break;
                case "use": if (parts.length == 2) return commands.use(parts[1]); break;
                case "inventory": if (parts.length == 1) return commands.inventory(); break;
                case "spawn":
                    if (parts.length == 2 || parts.length == 3) {
                        int count = parts.length == 3 ? Integer.parseInt(parts[2]) : 1;
                        if (count < 1 || count > 10) return "Quantidade permitida: 1 a 10.";
                        return commands.spawn(parts[1], count);
                    }
                    break;
                case "clear": if (parts.length == 1) return commands.clearEnemies(); break;
                case "level":
                    if (parts.length == 2) {
                        int count = Integer.parseInt(parts[1]);
                        if (count < 1 || count > 99) return "Níveis: 1 a 99.";
                        return commands.level(count);
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            return "Número inválido.";
        }
        return "Sintaxe inválida. Digite help.";
    }
}
