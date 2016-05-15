package com.github.overmind.yasir;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;

import java.io.IOException;

public class Yasir {
    public static Class<YasirLanguage> getLanguageClass() {
        return YasirLanguage.class;
    }

    public static TruffleRuntime rt() {
        return Truffle.getRuntime();
    }

    @ExplodeLoop
    public static MaterializedFrame atDepth(VirtualFrame frame, int depth) {
        CompilerAsserts.compilationConstant(depth);
        Frame here = frame;
        while (depth > 0) {
            here = (MaterializedFrame) here.getArguments()[0];
            --depth;
        }
        return (MaterializedFrame) here;
    }

    static class YasirContext {

    }

    static class YasirLanguage extends TruffleLanguage<YasirContext> {
        @Override
        protected YasirContext createContext(Env env) {
            return null;
        }

        @Override
        protected CallTarget parse(Source code, Node context, String... argumentNames) throws IOException {
            return null;
        }

        @Override
        protected Object findExportedSymbol(YasirContext context, String globalName, boolean onlyExplicit) {
            return null;
        }

        @Override
        protected Object getLanguageGlobal(YasirContext context) {
            return null;
        }

        @Override
        protected boolean isObjectOfLanguage(Object object) {
            return false;
        }

        @Override
        protected Object evalInContext(Source source, Node node, MaterializedFrame mFrame) throws IOException {
            return null;
        }
    }
}
