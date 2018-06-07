package daevil.menu.dependency;

import daevil.OSType;
import daevil.menu.Menu;
import daevil.property.Property;

import static daevil.property.Property.get;

public class JavaResolver extends Resolver {

    private String _urlPrefix = "http://cfmlprojects.org/artifacts/oracle/jre/";
    public final Property<String> urlPrefix = get(() -> _urlPrefix).set(value -> _urlPrefix = value);

    private String _version = "latest";
    public final Property<String> version = get(() -> _version).set(value -> _version = value);

    private String _prompt = "JAVA_HOME not set, and Java not detected in path.  Download a JRE? [Y/n]";
    public final Property<String> prompt = get(() -> _prompt).set(value -> _prompt = value);

    private String _jreZip;
    public final Property<String> jreZip = get(() -> _jreZip).set(value -> _jreZip = value);

    public JavaResolver(Menu menu) {
        super(menu);
    }

    private String url() {
        return urlPrefix.get() + version.get() + '-';
    }


    @Override
    public String generate(OSType osType) {
        if (OSType.NIX.typeOf(osType)) {
            return generateNix();
        } else if (OSType.WINDOWS.typeOf(osType)) {
            return generateWindows();
        } else {
            throw new IllegalArgumentException("Don't know how to handle osType of " + osType);
        }
    }

    public String generateNix() {
        return daevil.script.nix.bash.JavaResolver.template(urlPrefix.get(), prompt.get()).render().toString();
    }

    private String generateWindows() {
        return daevil.script.windows.batch.JavaResolver.template(menu.batchFileBuilder(), urlPrefix.get(), prompt.get()).render().toString();
    }
}
