package daevil.menu.dependency;

import daevil.OSType;
import daevil.menu.Menu;
import daevil.menu.MultiOSMenu;

import java.util.HashSet;
import java.util.Set;

public abstract class Resolver {

    Menu menu;

    public Resolver(Menu menu){
        this.menu = menu;
    }

    public String generate(OSType osType) {
        throw new NoSuchMethodError();
    }

}
