package de.unifreiburg.cs.proglang.jgs.constraints;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import de.unifreiburg.cs.proglang.jgs.constraints.CTypes.CType;
import de.unifreiburg.cs.proglang.jgs.constraints.TypeVars.TypeVar;
import polyglot.ast.Assign;

public final class Constraint<Level> {

    public enum Kind {
        LE("<="),
        COMP("~"),
        DIMPL("?=>");

        public final String str;
        Kind(String str) {
            this.str = str;
        }

    }

    Constraint(Kind kind, CType<Level> lhs, CType<Level> rhs) {
        this.kind = kind;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public final Kind kind;
    private final CType<Level> lhs;
    private final CType<Level> rhs;

    public boolean isSatisfied(TypeDomain<Level> types, Assignment<Level> a) {
        switch(this.kind) {

            case LE:
                return types.le(this.lhs.apply(a), this.rhs.apply(a));
            case COMP:
                TypeDomain.Type<Level> tlhs, trhs;
                tlhs = this.lhs.apply(a);
                trhs = this.rhs.apply(a);
                return types.le(tlhs, trhs) || types.le(trhs, tlhs);
            case DIMPL:
                // lhs = ? --> rhs <= ?
                return (!this.lhs.apply(a).equals(types.dyn())
                        || types.le(this.rhs.apply(a), types.dyn()));
            default: throw new RuntimeException("Impossible case!");
        }
    }

    /**
     *
     * @return true if this single constraint, in isolation, is satisfiable
     */
    // TODO: needs more tests
    public boolean isSatisfiable(TypeDomain<Level> types) {
        // first check if there are variable, which will make (binary) constraints satisfiable
        if (this.variables().count() != 0) {
            return true;
        } else {
            // no variables so check if it is satisfied with an empty assignment
            // TODO: (kind of a hack)
            return this.isSatisfied(types, Assignments.empty());
        }
    }


    public Constraint<Level> apply(Assignment<Level> a) {
        CType<Level> newLhs = this.lhs.applyWhenPossible(a);
        CType<Level> newRhs = this.rhs.applyWhenPossible(a);
        return Constraints.make(this.kind, newLhs, newRhs);
    }
    
    public Stream<TypeVar> variables() {
        return Stream.concat(lhs.variables(), rhs.variables());
    }

    public CType<Level> getLhs() {
        return this.lhs;
    }

    public CType<Level> getRhs() {
        return this.rhs;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", this.lhs, this.kind.str, this.rhs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Constraint<?> that = (Constraint<?>) o;

        if (kind != that.kind) return false;
        if (lhs != null ? !lhs.equals(that.lhs) : that.lhs != null) return false;
        return !(rhs != null ? !rhs.equals(that.rhs) : that.rhs != null);

    }

    @Override
    public int hashCode() {
        int result = kind != null ? kind.hashCode() : 0;
        result = 31 * result + (lhs != null ? lhs.hashCode() : 0);
        result = 31 * result + (rhs != null ? rhs.hashCode() : 0);
        return result;
    }
}
