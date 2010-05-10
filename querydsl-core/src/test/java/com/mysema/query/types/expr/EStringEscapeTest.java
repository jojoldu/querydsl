package com.mysema.query.types.expr;

import static org.junit.Assert.*;

import org.junit.Test;

public class EStringEscapeTest {

    @Test
    public void testEscapeForLike() {
        assertEquals("a\\%b\\_c", EStringEscape.escapeForLike(EStringConst.create("a%b_c")).toString());
    }

}
