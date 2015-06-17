// Copyright (c) 2014-2015 K Team. All Rights Reserved.

package org.kframework.kore.convertors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Test;
import org.junit.rules.TestName;
import org.kframework.attributes.Source;
import org.kframework.backend.java.kil.Definition;
import org.kframework.backend.java.kil.TermContext;
import org.kframework.backend.java.symbolic.InitializeRewriter;
import org.kframework.backend.java.symbolic.JavaSymbolicCommonModule;
import org.kframework.backend.java.symbolic.Stage;
import org.kframework.backend.java.symbolic.SymbolicRewriter;
import org.kframework.builtin.Sorts;
import org.kframework.definition.Module;
import org.kframework.kompile.CompiledDefinition;
import org.kframework.kompile.Kompile;
import org.kframework.kompile.KompileOptions;
import org.kframework.kore.K;
import org.kframework.krun.KRun;
import org.kframework.krun.api.io.FileSystem;
import org.kframework.krun.ioserver.filesystem.portable.PortableFileSystem;
import org.kframework.main.GlobalOptions;
import org.kframework.parser.ProductionReference;
import org.kframework.unparser.AddBrackets;
import org.kframework.unparser.KOREToTreeNodes;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;
import org.kframework.utils.inject.DefinitionScoped;
import org.kframework.utils.inject.RequestScoped;
import org.kframework.utils.inject.SimpleScope;
import org.kframework.utils.options.SMTOptions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;

import static org.kframework.Collections.*;
import static org.kframework.kore.KORE.*;
import static org.kframework.definition.Constructors.*;

public class TstBackendOnKORE_IT {

    @org.junit.Rule
    public TestName name = new TestName();

    protected File testResource(String baseName) throws URISyntaxException {
        return new File(TstTinyOnKORE_IT.class.getResource(baseName).toURI());
    }

    @Test
    public void kore_imp() throws IOException, URISyntaxException {

        String filename = "/convertor-tests/" + name.getMethodName() + ".k";

        File definitionFile = testResource(filename);
        KExceptionManager kem = new KExceptionManager(new GlobalOptions());
        try {
            CompiledDefinition compiledDef = new Kompile(new KompileOptions(), FileUtil.testFileUtil(), kem, false).run(definitionFile, "IMP", "IMP-SYNTAX", Sorts.K());

            BiFunction<String, Source, K> programParser = compiledDef.getProgramParser(kem);

            K program = programParser.apply(
                    "int s, n, .Ids; n = 10; while(0<=n) { s = s + n; n = n + -1; }", Source.apply("generated by " + getClass().getSimpleName()));
            KRun krun = new KRun(kem, FileUtil.testFileUtil());

            SimpleScope requestScope = new SimpleScope();
            Injector injector = Guice.createInjector(new JavaSymbolicCommonModule() {
                @Override
                protected void configure() {
                    super.configure();
                    bind(GlobalOptions.class).toInstance(new GlobalOptions());
                    bind(SMTOptions.class).toInstance(new SMTOptions());
                    bind(Stage.class).toInstance(Stage.REWRITING);
                    bind(FileSystem.class).to(PortableFileSystem.class);
                    bind(FileUtil.class).toInstance(FileUtil.testFileUtil());

                    bindScope(RequestScoped.class, requestScope);
                    bindScope(DefinitionScoped.class, requestScope);
                }
            });
            requestScope.enter();
            try {
                InitializeRewriter init = injector.getInstance(InitializeRewriter.class);
                K kResult = init.apply(compiledDef.executionModule()).execute(krun.plugConfigVars(compiledDef, Collections.singletonMap(KToken("$PGM", Sorts.KConfigVar()), program)), Optional.empty());
                Module unparsingModule = compiledDef.getParserModule(Module("UNPARSING", Set(compiledDef.executionModule(), compiledDef.syntaxModule(), compiledDef.getParsedDefinition().getModule("K-SORT-LATTICE").get(), compiledDef.getParsedDefinition().getModule("KSEQ-SYMBOLIC").get()), Set(), Att()));
                System.err.println(KOREToTreeNodes.toString(new AddBrackets(unparsingModule).addBrackets((ProductionReference) KOREToTreeNodes.apply(KOREToTreeNodes.up(kResult), unparsingModule))));
            } finally {
                requestScope.exit();
            }
        } finally {
            kem.print();
        }
    }

    private class GetSymbolicRewriter {
        private final Module module;
        private final KExceptionManager kem;
        private Definition definition;
        private TermContext termContext;
        private SymbolicRewriter rewriter;
        private SimpleScope requestScope;

        public GetSymbolicRewriter(Module module, KExceptionManager kem) {
            this.module = module;
            this.kem = kem;
        }

        public Definition getDefinition() {
            return definition;
        }

        public SymbolicRewriter getRewriter() {
            return rewriter;
        }

        public SimpleScope getRequestScope() { return requestScope; }

        public GetSymbolicRewriter invoke() {

            return this;
        }
    }
}
