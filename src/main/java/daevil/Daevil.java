package daevil;

import daevil.iconexe.IconExe;
import daevil.menu.Menu;
import daevil.menu.MenuOption;
import daevil.menu.MultiOSMenu;
import daevil.menu.dependency.JavaResolver;
import daevil.property.Property;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import net.gradleutil.gen.Generator;
import org.jsoftbiz.utils.OS;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

import static daevil.property.Property.get;

public class Daevil {

    public static final String LOGLEVEL_PROPERTY = "daevil.loglevel";
    private HashMap<String, String> tokens = new HashMap<>();

    private Path _dest;
    public final Property<Path> dest = get(() -> _dest).set(value -> _dest = value);

    private String _name;
    public final Property<String> name = get(() -> _name).set(value -> _name = value);

    private String _title;
    public final Property<String> title = get(() -> _title).set(value -> _title = value);

    private String _user;
    public final Property<String> user = get(() -> _user).set(value -> _user = value);

    private String _description;
    public final Property<String> description = get(() -> _description).set(value -> _description = value);

    private String _consoleSuccessOutput;
    public final Property<String> consoleSuccessOutput = get(() -> _consoleSuccessOutput).set(value -> _consoleSuccessOutput = value);

    private String _ctlScript;
    public final Property<String> ctlScript = get(() -> _ctlScript).set(value -> _ctlScript = value);

    private String _argStart = "start";
    public final Property<String> argStart = get(() -> _argStart).set(value -> _argStart = value);

    private String _argStartDarwin = "start-foreground";
    public final Property<String> argStartDarwin = get(() -> _argStartDarwin).set(value -> _argStartDarwin = value);

    private String _argStop = "stop";
    public final Property<String> argStop = get(() -> _argStop).set(value -> _argStop = value);

    private String _logFileOut;
    public final Property<String> logFileOut = get(() -> _logFileOut).set(value -> _logFileOut = value);

    private String _logFileErr;
    public final Property<String> logFileErr = get(() -> _logFileErr).set(value -> _logFileErr = value);

    private String _serviceImg;
    public final Property<String> serviceImg = get(() -> _serviceImg).set(value -> _serviceImg = value);

    private String _pidFile;
    public final Property<String> pidFile = get(() -> _pidFile).set(value -> _pidFile = value);

    private String _grepSuccessFile;
    public final Property<String> grepSuccessFile = get(() -> _grepSuccessFile).set(value -> _grepSuccessFile = value);

    private String _grepSuccessString;
    public final Property<String> grepSuccessString = get(() -> _grepSuccessString).set(value -> _grepSuccessString = value);

    private String _additional;
    public final Property<String> additional = get(() -> _additional).set(value -> _additional = value);

    private OS os = OS.getOs();
    private String osName = os.getName();
    private String osPlatformName = os.getPlatformName();
    private MultiOSMenu controlScript;

    public Daevil() {
        this("service");
    }

    public Daevil(String name) {
        log.debug(osName);
        log.debug(osPlatformName);
        this.name.set(name);
        ctlScript.set(name + "-ctl");
        controlScript = new MultiOSMenu(name + " Control Script");
        controlScript.fileName.set(name + "-ctl");
        addMenuOptions(controlScript);
    }

    Daevil(Path destination, String name, String title, String description, String consoleSuccessOutput, String ctlScript, String commandStart, String commandStop, String logFileOut, String logFileErr, String serviceImg) {
        this(name);
        dest.set(destination);
        this.title.set(title);
        this.description.set(description);
        this.consoleSuccessOutput.set(consoleSuccessOutput);
        this.ctlScript.set(ctlScript);
        this.argStart.set(commandStart);
        this.argStop.set(commandStop);
        this.logFileOut.set(logFileOut);
        this.logFileErr.set(logFileErr);
        this.serviceImg.set(serviceImg);
    }

    public MultiOSMenu controlScript() {
        return this.controlScript;
    }

    public Menu controlScript(OSType osType) {
        return this.controlScript.menu(osType);
    }

    private void addMenuOptions(MultiOSMenu menu) {
        menu.addOption("install-" + name.get(), "install the " + name.get() + " daemon")
                .command(OSType.WINDOWS, "CALL \"%SCRIPT_DIR%\\bin\\service\\\"install-service.bat install")
                .command(OSType.WINDOWS, "CALL \"%SCRIPT_DIR%\\bin\\service\\\"install-service.bat start")
                .command(OSType.NIX, "\"$SCRIPT_DIR/bin/daemon/\"install");

        menu.addOption("remove-" + name.get(), "remove the " + name.get() + " daemon")
                .command(OSType.WINDOWS, "CALL \"%SCRIPT_DIR%\\bin\\service\\\"install-service.bat uninstall")
                .command(OSType.NIX, "\"$SCRIPT_DIR/bin/daemon/\"remove");
    }

    public MenuOption addOption(String name, String description) {
        return controlScript.addOption(name, description);
    }

    public JavaResolver javaResolver() {
        JavaResolver resolver = new JavaResolver(controlScript);
        controlScript.resolver(resolver);
        log.debug("Added java resolver");
        return resolver;
    }

    private void updateTokens() {
        tokens.put("user", user.get());
        tokens.put("name", name.get());
        tokens.put("title", title.get());
        tokens.put("description", description.get());
        tokens.put("ctl-script", ctlScript.get());
        tokens.put("arg-start", argStart.get());
        tokens.put("arg-start-darwin", argStartDarwin.get());
        tokens.put("arg-stop", argStop.get());
        tokens.put("console-success-output", consoleSuccessOutput.get());
        tokens.put("log-file-out", logFileOut.get());
        tokens.put("log-file-err", logFileErr.get());
        tokens.put("pid-file", pidFile.get());
        tokens.put("grep-success-file", grepSuccessFile.get());
        tokens.put("grep-success-string", grepSuccessString.get());
        tokens.put("additional", additional.get());
    }

    public void generateScripts(OSType osType, Path dest) {
        if (osType.typeOf(OSType.NIX)) {
            generateNixScripts(dest);
        } else if (osType.typeOf(OSType.WINDOWS)) {
            generateWindowsScripts(dest);
        } else {
            throw new IllegalArgumentException("OSType is of unknown kind, cannot generate: " + osType.type());
        }
    }

    public void generateScriptsForHostOS(Path dest) {
        generateScripts(OSType.host(), dest);
    }

    void generateNixScripts(Path dest) {
        updateTokens();
        controlScript.generate(OSType.NIX, dest);
        String nixFiles = "/script/nix/";
        String nixTmplFiles = "/script/nix/tmpl";
        log.info("Generating *nix service scripts");
        ResourceUtil.copyResourcesReplaceTokens(nixFiles, Paths.get(dest.toString() + "/bin/daemon"), tokens, false, ".*", "");
        ResourceUtil.copyResources(nixTmplFiles, Paths.get(dest.toString() + "/bin/daemon/tmpl"), ".*", "");
    }

    void generateWindowsScripts(Path dest) {
        updateTokens();
        controlScript.generate(OSType.WINDOWS, dest);
        String windowsFiles = "/script/windows/";
        String procrunFiles = "/script/windows/procrun/";
        log.info("Generating windows service scripts");
        ResourceUtil.copyResourcesReplaceTokens(windowsFiles, dest, tokens, false, "install.*bat|README.txt", "");
        ResourceUtil.copyResources(procrunFiles + "amd64/", Paths.get(dest.toString(), "amd64/"), "prunsrv.*", "prunsrv," + name.get());
        ResourceUtil.copyResources(procrunFiles, dest, "prunsrv.*", "prunsrv," + name.get());
        ResourceUtil.copyResources(procrunFiles, dest, "prunmgr.*", "prunmgr," + name.get() + 'w');
        if (serviceImg.get() != null && !serviceImg.get().equals("")) {
            log.info("Loading image for windows service file: " + serviceImg.get());
            try {
                IconExe.setIcon(new File(dest.toFile(), name.get() + ".exe"), serviceImg.get());
                IconExe.setIcon(new File(dest.toFile(), name.get() + "w.exe"), serviceImg.get());
                IconExe.setIcon(new File(dest.toFile(), "amd64/" + name.get() + ".exe"), serviceImg.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            log.info("No image for windows service file.");
        }
    }

    static Path getJarPath() {
        try {
            return Paths.get(Daevil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static String render(String scriptPath, Map<String,Object> args){
        TemplateEngine engine = TemplateEngine.createPrecompiled(getJarPath(), ContentType.Plain, Daevil.class.getClassLoader(), "daevil");
        TemplateOutput out = new StringOutput();
        engine.render(scriptPath, args, out);
        return out.toString();
    }
    public static String render(String scriptPath, Object... args){
        TemplateEngine engine = TemplateEngine.createPrecompiled(getJarPath(), ContentType.Plain, Daevil.class.getClassLoader(), "daevil");
        TemplateOutput out = new StringOutput();
        engine.render(scriptPath, args, out);
        return out.toString();
    }

    interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    public static class log {
        static final String logLevel;
        static final Logger LOGGER;
        static final Level LEVEL;

        static {
//            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s%6$s%n");
            logLevel = System.getProperty(LOGLEVEL_PROPERTY, "INFO");
            LOGGER = Logger.getLogger(Daevil.class.getName());
            LEVEL = logLevel.equalsIgnoreCase("trace") ? Level.FINEST : Level.parse(logLevel);
            LOGGER.setLevel(LEVEL);
//            setLevel(LEVEL);
//            LOGGER.log(LEVEL, "SET LOG LEVEL TO " + LOGGER.getLevel());
            LOGGER.setUseParentHandlers(false);
            LOGGER.addHandler(new Handler() {
                @Override
                public void publish(LogRecord record) {
                    if (getFormatter() == null) {
                        setFormatter(new SimpleFormatter());
                    }
                    try {
                        String message = getFormatter().format(record);
                        if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                            System.err.write(message.getBytes());
                            System.err.flush();
                        } else {
                            System.out.write(message.getBytes());
                            System.out.flush();
                        }
                    } catch (Exception exception) {
                        reportError(null, exception, ErrorManager.FORMAT_FAILURE);
                    }
                }

                @Override
                public void close() throws SecurityException {
                }

                @Override
                public void flush() {
                }
            });
        }

        public static void debug(String message) {
            LOGGER.log(Level.CONFIG, message);
        }

        public static void trace(String message) {
            LOGGER.log(Level.FINER, message);
        }

        public static void info(String message) {
            LOGGER.log(Level.INFO, message);
        }

        public static void info(Path path) {
            info(path.toString());
        }

        public static void debug(Path path) {
            debug(path.toString());
        }

        public static void error(String message) {
            LOGGER.log(Level.SEVERE, message);
        }

        public static void setLevel(Level targetLevel) {
            Logger root = Logger.getLogger("");
            root.setLevel(targetLevel);
            for (Handler handler : root.getHandlers()) {
                handler.setLevel(targetLevel);
            }
            System.out.println("level set: " + targetLevel.getName());
        }


    }
}