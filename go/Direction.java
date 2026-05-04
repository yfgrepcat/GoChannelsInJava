package go;

/** Direction d'utilisation d'un canal, vis-à-vis des opérations in/out */
public enum Direction {
    In, Out;

    public static Direction inverse(Direction d) {
        switch (d) {
          case In : return Out;
          case Out: return In;
          default: return null; // pfeuh, compilateur incompétent
        }
    }
};
