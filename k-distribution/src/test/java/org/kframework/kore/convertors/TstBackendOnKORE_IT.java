// Copyright (c) 2014-2015 K Team. All Rights Reserved.

package org.kframework.kore.convertors;

import org.junit.Test;
import org.junit.rules.TestName;
import org.kframework.attributes.Source;
import org.kframework.definition.Module;
import org.kframework.kore.K;
import org.kframework.parser.ProductionReference;
import org.kframework.unparser.AddBrackets;
import org.kframework.unparser.KOREToTreeNodes;
import org.kframework.utils.KoreUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.junit.Assert.*;

public class TstBackendOnKORE_IT {

    @org.junit.Rule
    public TestName name = new TestName();

    protected File testResource(String baseName) throws URISyntaxException {
        return new File(TstTinyOnKORE_IT.class.getResource(baseName).toURI());
    }

    @Test
    public void kore_imp() throws IOException, URISyntaxException {
        String filename = "/convertor-tests/" + name.getMethodName() + ".k";
        KoreUtils utils = new KoreUtils(filename, "IMP", "IMP-SYNTAX");

        String pgm = "int s, n; n = 10; while(0<=n) { s = s + n; n = n + -1; }";

        K kResult = utils.stepRewrite(utils.getParsed(pgm, Source.apply("generated by " + getClass().getSimpleName())), Optional.<Integer>empty());

        Module unparsingModule = utils.getUnparsingModule();

        String actual = KOREToTreeNodes.toString(new AddBrackets(unparsingModule).addBrackets((ProductionReference) KOREToTreeNodes.apply(KOREToTreeNodes.up(unparsingModule, kResult), unparsingModule)));

        assertEquals("Execution failed", "<T> <k> .::K </k> <state> s |-> 55 n |-> -1 </state> </T>", actual);

    }
}
