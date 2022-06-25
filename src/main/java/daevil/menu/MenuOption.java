package daevil.menu;

import daevil.Daevil;
import daevil.OSType;
import daevil.property.Property;
import daevil.term.ProcessResult;

import java.util.*;

import static daevil.property.Property.get;

public class MenuOption {

    Map<OSType, List<String>> osCommands;
    private Menu menu;
    private String _name;
    public final Property<String> name = get(() -> _name)
            .set(value -> _name = value);

    private String _description;
    public final Property<String> description = get(() -> _description)
            .set(value -> _description = value);

    private Integer _number;
    public final Property<Integer> number = get(() -> _number)
            .set(value -> _number = value);

    MenuOption(Menu menu) {
        this.menu = menu;
    }

    MenuOption(Menu menu, String name, String description) {
        this.menu = menu;
        this.name.set(name);
        this.description.set(description);
        osCommands = new LinkedHashMap<>();
    }

    MenuOption(Menu menu, String name, String description, String command) {
        this.menu = menu;
        this.name.set(name);
        this.description.set(description);
        osCommands = new LinkedHashMap<>();
        command(command);
    }

    MenuOption(Menu menu, String name, String description, Map<OSType, List<String>> osCommands) {
        this.menu = menu;
        this.name.set(name);
        this.description.set(description);
        this.osCommands = osCommands;
    }

    public List<String> commandsish(OSType osType) {
        final List<String> matched = new ArrayList<>();
        osCommands.forEach((aClass, strings) -> {
            if (aClass.typeOf(osType))
                matched.addAll(strings);
        });
        if (matched.size() == 0 && osType.typeOf(OSType.NIX)) {
            osCommands.forEach((aClass, strings) -> {
                if (aClass == OSType.NIX)
                    matched.addAll(strings);
            });
        }
        return matched;
    }

    public List<String> commands(OSType osType) {
        osCommands.computeIfAbsent(osType, k -> new ArrayList<>());
        return osCommands.get(osType);
    }

    public String commandLines(OSType osType) {
        osCommands.computeIfAbsent(osType, k -> new ArrayList<>());
        return String.join("\n", osCommands.get(osType));
    }

    public MenuOption command(String command) {
        return command(OSType.ANY, command);
    }

    public MenuOption command(OSType osType, String command) {
        commands(osType).add(command);
        return this;
    }

    public List<ProcessResult> execute(OSType osType, boolean haltOnFailure) {
        return execute(osType, 120, haltOnFailure);
    }

    public List<ProcessResult> execute(OSType osType, int timeoutSeconds, boolean haltOnFailure) {
        final List<String> commands = commandsish(osType);
        final List<ProcessResult> results = new ArrayList<>();
        if (commands.size() == 0) {
            Daevil.log.error("No commands to run for osType: " + osType);
        }
        commands.forEach(command -> {
            results.add(osType.execute(command,null,null,timeoutSeconds,haltOnFailure));
        });
        return results;
    }



}
