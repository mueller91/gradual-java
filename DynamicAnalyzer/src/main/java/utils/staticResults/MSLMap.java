package utils.staticResults;

import de.unifreiburg.cs.proglang.jgs.instrumentation.Instantiation;
import de.unifreiburg.cs.proglang.jgs.instrumentation.Type;
import de.unifreiburg.cs.proglang.jgs.instrumentation.VarTyping;
import soot.Local;
import soot.SootMethod;
import soot.jimple.Stmt;
import utils.exceptions.InternalAnalyzerException;
import utils.staticResults.implementation.VarTypingImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * A _M_ethod, _S_tatement, _L_ocal Map
 */
public class MSLMap<T>
{
    private Map<SootMethod, Map<Stmt, Map<Local, T>>> map = new HashMap<>();

    public void put(SootMethod sm, Stmt stmt, Local l, T payload) {
        if (!map.containsKey(sm)) {
            map.put(sm, new HashMap<Stmt, Map<Local, T>>());
        }
        if (!map.get(sm).containsKey(stmt)) {
            map.get(sm).put(stmt, new HashMap<Local, T>());
        }
        if (map.get(sm).get(stmt).containsKey(l)) {
            throw new InternalAnalyzerException(String.format("Tried to add key( %s, %s, %s ), which is already present in static analysis result", sm, stmt, l));
        } else {
            map.get(sm).get(stmt).put(l, payload);
        }
    }

    public VarTyping getVar(SootMethod sm) {
      return new VarTypingImpl(map.get(sm));
    }

    public T get(SootMethod sm, Stmt stmt, Local l ) {
        boolean valuePresent = false;
        if (map.containsKey(sm)) {
            if ( map.get(sm).containsKey(stmt)) {
                if (map.get(sm).get(stmt).containsKey(l)) {
                    valuePresent = true;
                }
            }
        }
        if (valuePresent) {
            return map.get(sm).get(stmt).get(l);
        } else {
            throw new InternalAnalyzerException(String.format("Tried to retrieve key( %s, %s, %s ), which is not present in static analysis result", sm, stmt, l));
        }
    }


}
