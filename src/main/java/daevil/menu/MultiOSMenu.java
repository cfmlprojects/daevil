package daevil.menu;

import daevil.Daevil;
import daevil.OSType;
import daevil.ResourceUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class MultiOSMenu extends Menu {

    private Map<OSType, Menu> menus;

    public MultiOSMenu(String title) {
        super(title);
        menus = new HashMap<>();
    }

    public MenuOption addOption(String name, String description) {
        MenuOption option = new MenuOption(this, name, description);
        addOption(option);
        return option;
    }

    @Override
    public Menu addOption(String name, String description, String command) {
        MenuOption option = new MenuOption(this, name, description);
        option.command(OSType.ANY, command);
        return addOption(option);
    }

    @Override
    public Menu addOption(String name, String description, String... command) {
        MenuOption option = new MenuOption(this, name, description);
        Arrays.stream(command).forEach(cmd -> option.command(OSType.ANY, cmd));
        return addOption(option);
    }

    public String generate() {
        return menu(OSType.host()).generate();
    }

    public String generate(OSType osType) {
        return menu(osType).generate();
    }

    public Menu menu(OSType osType) {
        if (!(osType.typeOf(OSType.NIX) || osType.typeOf(OSType.WINDOWS))) {
            throw new IllegalArgumentException("OSType is of unknown kind: " + osType);
        }
        OSType superType = osType.typeOf(OSType.NIX) ? OSType.NIX : OSType.WINDOWS;
        Menu menu = menus.get(superType);
        if (menu == null) {
            if (superType == OSType.NIX) {
                menu = new BashMenu(title.get(), _options).resolvers(resolvers);
            } else {
                menu = new BatchFileMenu(title.get(), _options).resolvers(resolvers);
            }
        }
        menu.title(title.get()).options(_options).resolvers(resolvers).fileName.set(fileName.get());
        menus.put(superType, menu);
        return menu;
    }

    public Path generate(OSType osType, Path toDirectory) {
        if (!toDirectory.toFile().exists()) {
            toDirectory.toFile().mkdir();
        }
        String ext;
        if (osType.typeOf(OSType.NIX)) {
            ext = "";
        } else {
            ext = ".bat";
        }
        Path filePath = Paths.get(toDirectory.toString(), fileName.get() + ext);
        try {
            Daevil.log.info("Generating " + filePath);
            Files.write(filePath, generate(osType).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (osType.typeOf(OSType.NIX)) {
            ResourceUtil.markExecutable(filePath);
        }
        return filePath;
    }

    public List<Path> generate(Path path) {
        List<Path> paths = new ArrayList<>();
        paths.add(generate(OSType.NIX, path));
        paths.add(generate(OSType.WINDOWS, path));
        return paths;
    }

    public OSType.ProcessResult execute(Path path, String... input) {
        return execute(path, OSType.host(), input);
    }

    public OSType.ProcessResult execute(Path path, OSType osType, String... input) {
        generate(osType, path);
        String scriptPath;
        if (osType == OSType.WINDOWS) {
            scriptPath = fileName + ".bat";
        } else {
            scriptPath = fileName.get();
        }
        path = Paths.get(path.toAbsolutePath().toString(), scriptPath);
        Daevil.log.debug("Executing: " + path);
        return osType.execute(path.toString(), null, Arrays.asList(input), 10, false);
    }

    /**
     * Just for testing resolvers
     *
     * @param path
     * @param osType
     * @return
     */
    public OSType.ProcessResult executeResolvers(Path path, OSType osType) {
        try {
            Daevil.log.info("Generating resolver script: " + path);
            Files.write(path, generateResolverText(osType).getBytes());
            String scriptPath;
            if (osType == OSType.WINDOWS) {
                scriptPath = "resolvers" + ".bat";
            } else {
                scriptPath = "resolvers";
            }
            path = Paths.get(path.toAbsolutePath().toString(), scriptPath);
            Daevil.log.debug("Executing: " + path);
            return osType.execute(path.toString(), null, null, 10, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public BatchFileBuilder batchFileBuilder() {
        return menu(OSType.WINDOWS).batchFileBuilder();
    }

}