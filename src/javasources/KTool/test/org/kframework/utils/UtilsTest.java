// Copyright (c) 2014 K Team. All Rights Reserved.
package org.kframework.utils;

import junit.framework.Assert;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void StringUtilUnquote() throws Exception {
        Assert.assertTrue(StringUtil.unquoteString("\"\\n\"").equals("\n"));
        Assert.assertTrue(StringUtil.unquoteString("\"\\r\"").equals("\r"));
        Assert.assertTrue(StringUtil.unquoteString("\"\\f\"").equals("\f"));
        Assert.assertTrue(StringUtil.unquoteString("\"\\t\"").equals("\t"));

        Assert.assertTrue(StringUtil.unquoteString("\"\\x20\"").equals(" "));
        Assert.assertTrue(StringUtil.unquoteString("\"\\u0020\"").equals(" "));
        Assert.assertTrue(StringUtil.unquoteString("\"\\U00000020\"").equals(" "));

        Assert.assertTrue(StringUtil.unquoteString("\"\\U00000020\"").equals(" "));

        try {
            StringUtil.unquoteString("\"\\U00110000\"");
            throw new AssertionError();
        } catch (IllegalArgumentException e) {
        }
        try {
            StringUtil.unquoteString("\"\\U0000d801\"");
            throw new AssertionError();
        } catch (IllegalArgumentException e) {
        }
    }
}
