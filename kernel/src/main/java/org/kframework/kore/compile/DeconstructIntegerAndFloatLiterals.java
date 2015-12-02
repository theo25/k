// Copyright (c) 2015 K Team. All Rights Reserved.
package org.kframework.kore.compile;

import org.kframework.builtin.BooleanUtils;
import org.kframework.builtin.Sorts;
import org.kframework.definition.Context;
import org.kframework.definition.Rule;
import org.kframework.definition.Sentence;
import org.kframework.kil.Attribute;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.KRewrite;
import org.kframework.kore.KToken;
import org.kframework.kore.KVariable;
import org.kframework.kore.Sort;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.kframework.definition.Constructors.Att;
import static org.kframework.definition.Constructors.*;
import static org.kframework.kore.KORE.*;

/**
 * Transforms patterns in the LHS of rules which have tokens of sort Int or Float
 * into side conditions generating equality over a reconstructed value.
 * Thus,
 * <p>
 * rule 5 => .K
 * <p>
 * becomes
 * <p>
 * rule I:Int => .K when I ==K 5
 */
public class DeconstructIntegerAndFloatLiterals {

    private Set<KApply> state = new HashSet<>();
    private Set<KVariable> vars = new HashSet<>();

    void reset() {
        state.clear();
        vars.clear();
    }

    void gatherVars(K term) {
        new VisitKORE() {
            @Override
            public Void apply(KVariable v) {
                vars.add(v);
                return super.apply(v);
            }
        }.apply(term);
    }

    public Sentence convert(Sentence s) {
        if (s.att().contains(Attribute.MACRO_KEY)) {
            return s;
        }
        if (s instanceof Rule) {
            return convert((Rule) s);
        } else if (s instanceof Context) {
            return convert((Context) s);
        } else {
            return s;
        }
    }

    private Rule convert(Rule rule) {
        reset();
        gatherVars(rule.body());
        gatherVars(rule.requires());
        gatherVars(rule.ensures());
        K body = convert(rule.body());
        K requires = convertLookups(rule.requires());
        return Rule(
                body,
                addSideCondition(requires),
                rule.ensures(),
                rule.att());
    }

    private K convertLookups(K requires) {
        return new Transformer(false).apply(requires);
    }

    private Context convert(Context context) {
        reset();
        gatherVars(context.body());
        gatherVars(context.requires());
        K body = convert(context.body());
        return Context(
                body,
                addSideCondition(context.requires()),
                context.att());
    }

    private int counter = 0;

    KVariable newDotVariable(Sort sort) {
        KVariable newLabel;
        do {
            newLabel = KVariable("_" + (counter++), Att().add("sort", sort.name()));
        } while (vars.contains(newLabel));
        vars.add(newLabel);
        return newLabel;
    }

    K addSideCondition(K requires) {
        Optional<KApply> sideCondition = state.stream().reduce(BooleanUtils::and);
        if (!sideCondition.isPresent()) {
            return requires;
        } else if (requires.equals(BooleanUtils.TRUE) && sideCondition.isPresent()) {
            return sideCondition.get();
        } else {
            return BooleanUtils.and(requires, sideCondition.get());
        }
    }

    private K convert(K term) {
        return new Transformer(true).apply(term);
    }

    private class Transformer extends TransformKORE {

        @Override
        public K apply(KToken k) {
            if (lhs) {
                if (k.sort().equals(Sorts.Int()) || k.sort().equals(Sorts.Float())) {
                    KVariable var = newDotVariable(k.sort());
                    state.add(KApply(KLabel("_==" + k.sort().name() + "_"), var, k));
                    return var;
                }
            }
            return super.apply(k);
        }

        private boolean lhs;

        public Transformer(boolean lhs) {
            this.lhs = lhs;
        }

        @Override
        public K apply(KApply k) {
            if (ConvertDataStructureToLookup.isLookupKLabel(k)) {
                assert k.klist().size() == 2;
                K r = apply(k.klist().items().get(1));
                lhs = true;
                K l = apply(k.klist().items().get(0));
                lhs = false;
                if (l != k.klist().items().get(0) || r != k.klist().items().get(1)) {
                    return KApply(k.klabel(), l, r);
                } else {
                    return k;
                }
            }
            return super.apply(k);
        }

        @Override
        public K apply(KRewrite k) {
            K l = apply(k.left());
            lhs = false;
            K r = apply(k.right());
            lhs = true;
            if (l != k.left() || r != k.right()) {
                return KRewrite(l, r, k.att());
            } else {
                return k;
            }
        }
    }
}
