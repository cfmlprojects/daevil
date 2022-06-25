package daevil;

import daevil.term.Console;
import daevil.term.ProcessResult;
import org.jsoftbiz.utils.OS;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public enum OSType {

    ANY("ANY"),
    NIX("NIX"),
    NIX_DEBIANISH("NIX"),
    NIX_RHELISH("NIX"),
    NIX_DARWINISH("NIX"),
    WINDOWS("WINDOWS");

    private static String ttyConfig;
    private final String type;

    OSType(String type) {
        this.type = type;
    }

    public static OSType host() {
        OS os = OS.getOs();

        if (os.getName().contains("Windows"))
            return OSType.WINDOWS;

        if (Files.exists(Paths.get("/Library/LaunchDaemons/")))
            return OSType.NIX_DARWINISH;

        if (Files.exists(Paths.get("/lib/lsb/init-functions")))
            return OSType.NIX_DEBIANISH;

        if (Files.exists(Paths.get("/etc/init.d/functions")))
            return OSType.NIX_RHELISH;

        if (os.getName().contains("nix"))
            return OSType.NIX;

        throw new IllegalArgumentException("Unknown os type: " + os.getPlatformName());
    }

    public String type() {
        return type;
    }

    public boolean typeOf(OSType osType) {
        switch (osType) {
            case ANY:
                return true;
            case NIX:
                return type.equals("NIX") || this == ANY;
            case NIX_DARWINISH:
                return this == NIX_DARWINISH || this == ANY;
            case NIX_DEBIANISH:
                return this == NIX_DEBIANISH || this == ANY;
            case NIX_RHELISH:
                return this == NIX_RHELISH || this == ANY;
            case WINDOWS:
                return this == WINDOWS || this == ANY;
        }
        return false;
    }

    public ProcessResult execute(Path executable) {
        File scriptFile = executable.toFile();
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Executable not found for osType: " + executable + " : " + this);
        }
        return execute(executable.toAbsolutePath().toString(), null, null, 10, false);
    }

    public ProcessResult execute(String executeThis, Map<String, String> input) {
        return execute(executeThis, null, input, 10, false);
    }


    public ProcessResult execute(String executeThis, List<String> arguments, Map<String, String> input, int timeoutSeconds, boolean haltOnFailure) {
        Console console = new Console();
        return console.execute(host(), this, executeThis, arguments, input, timeoutSeconds, haltOnFailure);
    }

}