package me.juhanlol;

import org.apache.zeppelin.interpreter.Interpreter;
import org.apache.zeppelin.interpreter.InterpreterPropertyBuilder;

import java.util.Properties;

public class ScalaInterpreter extends Ammonium {
    static {
        Interpreter.register(
                "scala",
                "scala",
                ScalaInterpreter.class.getName(),
                new InterpreterPropertyBuilder()
                        .add(
                                "dummy",
                                "dummy value",
                                "dummy value documentation")
                        .build()
        );
    }

    public ScalaInterpreter(Properties property) {
        super(property);
    }
}