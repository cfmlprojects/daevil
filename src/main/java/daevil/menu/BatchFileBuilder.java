package daevil.menu;

import daevil.Daevil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class BatchFileBuilder {

    StringBuilder content;
    HashSet<String> functionBucket;

    public BatchFileBuilder() {
        this(false);
    }

    public BatchFileBuilder(Boolean echo) {
        String echoVar = Optional.ofNullable(echo).filter(p -> p).map(m -> "on").orElse("off");
        content = new StringBuilder("@echo " + echoVar + '\n'
                + "setLocal EnableDelayedExpansion" + '\n'
                + "IF \"x%SCRIPT_DIR%\" == \"x\" SET SCRIPT_DIR=%~dp0%" + '\n'
                + "IF \"!SCRIPT_DIR:~-1!\"==\"\\\\\" SET SCRIPT_DIR=!SCRIPT_DIR:~,-1!" + '\n'
                + "SET BASE_SCRIPT_DIR=%SCRIPT_DIR%" + '\n');
        functionBucket = new HashSet<>();
    }

    public static String powerShellString(String scriptPath, Object... args) {
        String out = Daevil.render("daevil/script/windows/powershell/" + scriptPath + ".jte", args);
        String script = out.trim().replaceAll("[\r|\n]+(\\s*)", "; ").replace("\"", "\\\"");
        script = script.replace("{;", "{").replace("};", "}");
        return "powershell.exe -Noninteractive -NoProfile -command \"" + script + "\"";
    }

    public static String powerShellVariable(String scriptPath, String variableName, Object... args) {
        String out = Daevil.render("daevil/script/windows/powershell/" + scriptPath + ".jte", args);
        String script = out.trim().replaceAll("[\r|\n]", "; ^\n");
        return "set \"ps_"+ scriptPath + "_Command=powershell -executionpolicy remotesigned -Noninteractive -NoProfile -command \"" + script + "\"\"" + '\n' +
                "    for /f \"usebackq delims=\" %%G in (`%ps_"+ scriptPath + "_Command%`) do (set \"" + variableName + "=%%G\")";
    }

    public HashSet<String> getFunctionBucket() {
        return functionBucket;
    }

    public StringBuilder append(String string) {
        return content.append(string);
    }

    public String call(String function, String... args) {
        String out = Daevil.render("daevil/script/windows/batch/function/" + function + ".jte", Collections.singletonMap("builder",this));
        getFunctionBucket().add(out);
        return "CALL :" + function + " \"" + String.join("\" \"", args) + '"';
    }

    public String toString() {
        return content.append("\nendlocal\nEXIT /B 0\n\n::*** FUNCTIONS ***\n").append(String.join("\n", getFunctionBucket())).toString();
    }

    public boolean write(Path filePath) {
        try {
            Daevil.log.info("Writing " + filePath);
            Files.write(filePath, toString().getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean writeDebug(Path filePath) {
        try {
            Daevil.log.info("Writing " + filePath);
            final StringBuilder debugVersion = new StringBuilder();
            final AtomicInteger count = new AtomicInteger();
            Arrays.stream(toString().split("\n")).forEach(line -> {
                        try {
                            debugVersion
                                    .append("\necho.Line ").append(count.incrementAndGet()).append(System.lineSeparator())
                                    .append(line);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
            Files.write(filePath, debugVersion.toString().getBytes());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
