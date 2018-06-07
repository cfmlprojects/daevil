package daevil.menu;

import daevil.Daevil;
import daevil.OSType;
import daevil.ResourceUtil;
import daevil.menu.dependency.JavaResolver;
import daevil.menu.dependency.Resolver;
import daevil.property.Property;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static daevil.property.Property.get;

public abstract class Menu {

    List<MenuOption> _options;
    int optionCount = 0;
    List<Resolver> resolvers;
    // for windows batch functions


    private String _title;
    public final Property<String> title = get(() -> _title)
            .set(value -> _title = value);

    private String _fileName;
    public final Property<String> fileName = get(() -> _fileName)
            .set(value -> _fileName = value);

    public Menu(String title) {
        _options = new ArrayList<>();
        resolvers = new ArrayList<>();
        this.title.set(title);
        fileName.set(safeName(title));
    }

    public Menu(String title, List<MenuOption> options) {
        this(title);
        _options = options;
    }

    public List<Resolver> resolvers() {
        return resolvers;
    }

    public Menu resolvers(List<Resolver> resolvers) {
        this.resolvers = resolvers;
        return this;
    }

    public Menu resolver(Resolver resolver) {
        this.resolvers.add(resolver);
        return this;
    }

    public JavaResolver javaResolver() {
        JavaResolver resolver = new JavaResolver(this);
        resolver(resolver);
        return resolver;
    }


    String safeName(String name) {
        return name.replace("-", "_").replace(".", "_");
    }

    public Menu addOption(MenuOption option) {
        if (getOption(option.name.get()) != null) {
            throw new RuntimeException("Each option must be unique, duplicate: " + option.name.get());
        }
        if (!option.name.get().equals("separator")) {
            optionCount++;
            option.number.set(optionCount);
        }
        options().add(option);
        return this;
    }

    public Menu title(String title) {
        _title = title;
        return this;
    }

    public List<MenuOption> options() {
        return _options;
    }

    public Menu options(List<MenuOption> options) {
        _options = options;
        return this;
    }

    public Menu addOption(String name, String description, String command) {
        MenuOption option = new MenuOption(this, name, description, command);
        return addOption(option);
    }

    public MenuOption getOption(final String optionName) {
        List<MenuOption> found = options().stream().filter(menuOption -> menuOption.name.get() == optionName).collect(Collectors.toList());
        if (found.size() > 0)
            return found.get(0);
        return null;
    }

    public Menu addOption(String name, String description, String... command) {
        MenuOption option = new MenuOption(this, name, description, String.join("\n", command));
        return addOption(option);
    }

    public List<Path> generate(Path dest) {
        List<Path> paths = new ArrayList<>();
        String script = generate();
        try {
            Files.write(dest, script.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        paths.add(dest);
        return paths;
    }

    public String generate() {
        return displayCommands(OSType.ANY);
    }


    public String displayCommands(OSType osType) {
        StringBuilder sb = new StringBuilder();
        sb.append(title.get()).append('\n');
        resolvers().forEach(resolver -> System.out.println("Resolver: " + resolver.getClass().getName() + '\n'));
        _options.forEach(option -> {
            sb.append(option.number.get()).append(") ").append(option.name.get()).append(" Description: ").append(option.description.get()).append('\n');
            option.commandsish(osType).forEach(command ->
                    sb.append("  " + " Command: ").append(command).append('\n')
            );
        });
        return sb.toString();
    }

    String usageString(String suffix, String usageSeparator) {
        final StringBuilder sb = new StringBuilder(suffix);
        _options.forEach(option -> {
            if (!option.name.get().equals("separator"))
                sb.append(option.name.get()).append(usageSeparator);
        });
        return sb.toString().substring(0, sb.length() - 1) + ']';
    }

    public String generateResolverText(OSType osType) {
        final StringBuilder resolverText = new StringBuilder();
        resolvers().forEach(resolver -> System.out.println("Resolver: " + resolver.getClass().getName()));
        resolvers().forEach(resolver -> resolverText.append(resolver.generate(osType)));
        return resolverText.append('\n').toString();
    }

    public abstract BatchFileBuilder batchFileBuilder();
}