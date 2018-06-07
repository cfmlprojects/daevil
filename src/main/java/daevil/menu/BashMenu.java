package daevil.menu;

import daevil.OSType;

import java.util.List;

public class BashMenu extends Menu {

    public BashMenu(String title) {
        super(title);
    }

    BashMenu(String title, List<MenuOption> options) {
        super(title, options);
    }

    @Override
    public String generate() {
        String usage = usageString("usage: $(basename $0) [", "|");

        final StringBuilder menuText = new StringBuilder();
        menuText.append("#!/bin/bash\n" +
                "export SCRIPT_DIR=\"$( cd \"$( dirname \"${BASH_SOURCE[0]}\" )\" && pwd )\"" + '\n'
                + "export EXIT_CODE=0" + '\n');

        menuText.append(generateResolverText(OSType.NIX));

        _options.forEach(option -> {
            menuText.append("function " + safeName(option.name.get()) + "() {" + '\n');
            menuText.append("    " + option.commandLines(OSType.NIX) + " \"$@\"" + '\n');
            menuText.append("    export EXIT_CODE=$?" + '\n');
            menuText.append("}" + '\n');
        });
        menuText.append("#skip menu if arguments are passed" + '\n'
                + "if [ -z \"$1\" ]; then" + '\n'
                + "while [ answer != \"0\" ]" + '\n'
                + "do" + '\n'
                + "clear" + '\n'
                + "cd \"$SCRIPT_DIR\"" + '\n'
                + "echo  \"" + title.get() + "\"" + '\n'
                + "echo \"  " + usage + "\"\n"
                + "echo" + '\n'
                + "echo \"Select from the following options\"" + '\n'
                + "echo" + '\n'
                + "echo \"  0  EXIT\"" + '\n');

        _options.forEach(option -> {
            if (option.name.get().equals("separator")) {
                menuText.append("echo \"  ************************************************************\"" + '\n');
            } else {
                menuText.append("echo \"  " + option.number.get() + "  " + option.name.get() + " (" + option.description.get() + ")\"" + '\n');
            }
        });

        menuText.append("echo" + '\n');
        menuText.append("read -p \"Option number: \" answer" + '\n');
        menuText.append("  case $answer in" + '\n');
        menuText.append("    0) break ;;" + '\n');
        _options.forEach(option -> {
            if (option.name.get().equals("separator")) {
            } else {
                menuText.append("    " + option.number.get() + ") " + safeName(option.name.get()) + '\n');
                menuText.append("    " + ";;" + '\n');
            }
        });
        menuText.append("    *) break ;;" + '\n');
        menuText.append("    esac" + '\n');
        menuText.append(" echo \"press RETURN for menu\"" + '\n');
        menuText.append(" read key" + '\n');
        menuText.append("done" + '\n');
        menuText.append("exit 0" + '\n');
        menuText.append("fi" + '\n');
        menuText.append("#arguments were passed in" + '\n');
        _options.forEach(option -> {
            if (option.name.get().equals("separator")) {
            } else {
                menuText.append("if [ $1 = " + option.name.get() + " ]; then" + '\n');
                menuText.append("   " + safeName(option.name.get()) + " ${@:2}\n");
                menuText.append("   exit $EXIT_CODE" + '\n');
                menuText.append("fi" + '\n');
            }
        });
        return menuText.toString();

    }

    public BatchFileBuilder batchFileBuilder() {
        throw new UnsupportedOperationException("This is a bash file, not a batch file.");
    }

}