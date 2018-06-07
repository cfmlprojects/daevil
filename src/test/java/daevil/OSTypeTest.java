package daevil;


import org.junit.jupiter.api.Test;

import static daevil.OSType.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OSTypeTest {

    @Test
    void ofType() {
        assertTrue(NIX.typeOf(ANY));
        assertFalse(NIX.typeOf(WINDOWS));

        assertTrue(NIX_DARWINISH.typeOf(NIX));
        assertTrue(NIX_DARWINISH.typeOf(ANY));
        assertFalse(NIX_DARWINISH.typeOf(WINDOWS));
        assertFalse(NIX.typeOf(NIX_DARWINISH));

        assertTrue(NIX_DEBIANISH.typeOf(NIX));
        assertTrue(NIX_DEBIANISH.typeOf(ANY));
        assertFalse(NIX_DEBIANISH.typeOf(WINDOWS));
        assertFalse(NIX.typeOf(NIX_DEBIANISH));

        assertTrue(WINDOWS.typeOf(WINDOWS));
        assertTrue(WINDOWS.typeOf(ANY));
        assertFalse(WINDOWS.typeOf(NIX));

    }


}
