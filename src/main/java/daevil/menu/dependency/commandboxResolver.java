package daevil.menu.dependency;

import daevil.OSType;
import daevil.menu.Menu;
import daevil.property.Property;

import static daevil.property.Property.get;

public class commandboxResolver extends Resolver {

    private String _urlPrefix = "https://www.ortussolutions.com/parent/download/commandbox/type/windows-jre";
    public final Property<String> urlPrefix = get(() -> _urlPrefix).set(value -> _urlPrefix = value);

    private String _version = "latest";
    public final Property<String> version = get(() -> _version).set(value -> _version = value);

    private String _prompt = "COMMANDBOX_HOME not set, and not detected in path.  Download CommandBox? [Y/n]";
    public final Property<String> prompt = get(() -> _prompt).set(value -> _prompt = value);

    commandboxResolver(Menu menu) {
        super(menu);
    }

    private String url(){
        return urlPrefix.get() + version.get() + '-';
    }

    @Override
    public String generate(OSType osType) {
        if(OSType.NIX.typeOf(osType)){
            return daevil.script.nix.bash.JavaResolver.template(urlPrefix.get(), prompt.get()).render().toString();
        } else if(OSType.WINDOWS.typeOf(osType)){
            return daevil.script.windows.batch.JavaResolver.template(menu.batchFileBuilder(), urlPrefix.get(), prompt.get()).render().toString();
        } else {
            throw new IllegalArgumentException("Don't know how to handle osType of " + osType);
        }
    }
}
