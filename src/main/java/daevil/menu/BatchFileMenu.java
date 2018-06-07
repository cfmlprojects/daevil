package daevil.menu;

import daevil.OSType;

import java.util.HashSet;
import java.util.List;

public class BatchFileMenu extends Menu {

    HashSet<String> functionBucket;

    private final BatchFileBuilder batchFileBuilder = new BatchFileBuilder();

    public BatchFileMenu(String title) {
        super(title);
        functionBucket = new HashSet<>();
    }

    public BatchFileMenu(String title, List<MenuOption> options) {
        super(title, options);
        functionBucket = new HashSet<>();
    }

    public HashSet<String> functionBucket(){
        return functionBucket;
    }

    public BatchFileBuilder batchFileBuilder(){
        return batchFileBuilder;
    }


    @Override
    public String generate() {

        String usage = usageString("usage: %~n0%~x0 ^[", "^|");

        batchFileBuilder.append(generateResolverText(OSType.WINDOWS)
                + "REM get all args after first for passing extra args" + '\n'
                + "set FIRSTARG=%1" + '\n'
                + "set RESTVAR=" + '\n'
                + "shift" + '\n'
                + ":loop1" + '\n'
                + "if \"%1\"==\"\" goto after_loop" + '\n'
                + "set RESTVAR=%RESTVAR% %1" + '\n'
                + "shift" + '\n'
                + "goto loop1" + '\n'
                + ":after_loop" + '\n'
                + "if NOT \"%FIRSTARG%\" == \"\" goto RUNCOMMAND" + '\n'
                + "goto MENU" + "\n\n"
                + "REM FUNCTIONS FOR COMMANDS" + '\n');

        // function for each command
        _options.forEach(option -> {
            if (option.name.get().equals("separator")) {
            } else {
                batchFileBuilder.append("REM *** " + option.name.get() + " *** \n"
                        + ":" + option.name.get() + '\n'
//             + "setlocal" + '\n'
//             + "SET DIR=%~dp0%:~0,-1" + '\n'
                        + "cls" + '\n'
                        + "echo " + option.name.get() + '\n'
                        + option.commandLines(OSType.WINDOWS) + '\n'
//             +'endlocal' + '\n'
                        + "goto %GOTONEXT%" + '\n'
                        + "::" + "\n\n");

            }
        });

        batchFileBuilder.append(":MENU" + '\n'
                + "SET GOTONEXT=PAUSEMENU" + '\n'
                + "cls" + '\n'
                + "SET SCRIPT_DIR=%BASE_SCRIPT_DIR%" + '\n'
                + "CD %SCRIPT_DIR%" + '\n'
                + "echo." + '\n'
                + "echo  " + title.get() + '\n'
                + "echo    " + usage + '\n'
                + "echo." + '\n'
                + "echo    0 EXIT" + '\n');

        _options.forEach(option -> {
            if (option.name.get().equals("separator")) {
                batchFileBuilder.append("echo    ************************************************************" + '\n');
            } else {
                batchFileBuilder.append("echo    " + option.number.get() + " " + option.name.get() + " (" + option.description.get() + ")\n");
            }
        });

        batchFileBuilder.append("echo." + '\n'
                + "echo." + '\n'
                + "set choice=" + '\n'
                + "set /p choice=  Enter option number :" + '\n'
                + "echo." + '\n'
                + "REM trim whitespace" + '\n'
                + "for /f \"tokens=* delims= \" %%a in (\"%choice%\") do set choice=%%a" + '\n'
                + "for /l %%a in (1,1,100) do if \"!choice:~-1!\"==\" \" set choice=!choice:~0,-1!" + '\n'
                + "if '%choice%'=='0' goto BYE" + '\n');

        _options.forEach(option -> {
            if (!option.name.get().equals("separator")) {
                batchFileBuilder.append("if '%choice%'=='" + option.number.get() + "' goto " + option.name.get() + '\n');
            }
        });

        batchFileBuilder.append("::" + '\n'
                + "echo." + '\n'
                + "echo." + '\n'
                + "echo \"%choice%\" is not a valid option - try again" + '\n'
                + "echo." + '\n'
                + "pause" + '\n'
                + "goto MENU" + '\n'
                + "::" + '\n');

        batchFileBuilder.append(":RUNCOMMAND" + '\n'
                + "SET GOTONEXT=BYE" + '\n');

        _options.forEach(option -> {
            if (!option.name.get().equals("separator")) {
                batchFileBuilder.append("if \"%FIRSTARG%\" == \"" + option.name.get() + "\" goto " + option.name.get() + '\n');
            }
        });

        batchFileBuilder.append("echo Unknown option %FIRSTARG%" + '\n'
                + "goto BYE" + '\n'
                + "::" + '\n'
                + ":PAUSEMENU" + '\n'
                + "set choice=" + '\n'
//                + "echo       press any key ..." + '\n'
                + "pause" + '\n'
                + "goto MENU" + '\n'
                + "::" + '\n'
                + ":BYE" + '\n'
                + "::" + '\n');

        batchFileBuilder.append("exit /B 0");
        if(resolvers().size() > 0){
//            functionBucket().add(daevil.script.windows.batch.function.bat_prompt_confirm.template().render().toString());
//            functionBucket().add(daevil.script.windows.batch.function.bat_url_file.template().render().toString());
//            functionBucket().add(daevil.script.windows.batch.function.bat_md5_file.template().render().toString());
            String functions = String.join("\n", functionBucket());
            batchFileBuilder.append("\n::Functions\n").append(functions);
        }

        // windows line endings and return
        return batchFileBuilder.toString().replace("\n", "\r\n");
    }


}