package jgstestclasses;

import de.unifreiburg.cs.proglang.jgs.support.Casts;
import de.unifreiburg.cs.proglang.jgs.support.Constraints;
import de.unifreiburg.cs.proglang.jgs.support.Effects;

public class SimpleCast_Fail2 {

    @Constraints({"LOW <= @0"})
    @Effects("LOW")
    public static void main(String[] args) {
        int v = 42;
        int x = Casts.cast("HIGH ~> ?", v);
        System.out.println(Casts.cast("? ~> LOW", x));
    }
}
