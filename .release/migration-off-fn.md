* Migrated the `lib` module away from the custom `Fn` class to standard Kotlin lambdas.
    * Introduced `ExecutionScope` to provide contextual parameters to lambdas during execution, especially in distributed environments.
    * Added custom serializers in the `exe` module for all migrated components to maintain backward compatibility with existing distributed execution infrastructure.
    * Migrated major components including `map`, `merge`, `window`, `resample`, `flatten`, `flatMap`, `toCsv`, `toWav`, `toTable`, and `out`.
