### Migration of `Fn` Inheritants

This document tracks the migration of `Fn` inheritants to standard Kotlin functional interfaces (lambdas) as part of the effort to phase out the `Fn` class in the `lib` module.

#### Production Code

- [x] `io.wavebeans.lib.stream.SincResampleFn`
  - **File**: `lib/src/commonMain/kotlin/io/wavebeans/lib/stream/SincResampleFn.kt`
  - **Status**: Migrated to a simple class and updated `sincResampleFunc`.
  - **Todo**:
    - [x] Update `SincResampleFn` constructor to use lambdas instead of `Fn`.
    - [x] Update `SincResampleFn` to use lambdas internally.
    - [x] Update `sincResampleFunc` to use lambdas.
    - [ ] Create a custom serializer in `exe` module (if not already handled by `ResampleStreamParamsSerializer`).

- [x] `io.wavebeans.lib.stream.SimpleResampleFn`
  - **File**: `lib/src/commonMain/kotlin/io/wavebeans/lib/stream/SimpleResampleFn.kt`
  - **Status**: Migrated to a simple class with private `reduceFn` parameter and no default value.
  - **Todo**:
    - [x] Change `SimpleResampleFn` to not inherit from `Fn`.
    - [x] Update `reduceFn` to be a lambda.
    - [x] Remove `Fn`-based constructors.

- [x] `io.wavebeans.lib.io.SampleCsvFn`
  - **File**: `lib/src/commonMain/kotlin/io/wavebeans/lib/io/CsvSampleStreamOutput.kt`
  - **Status**: Migrated to a simple class with `invoke` operator.
  - **Todo**:
    - [x] Change `SampleCsvFn` to not inherit from `Fn`.
    - [x] Update usages in `toCsv` extensions to use it as a regular class or lambda.

#### Test Code

- [x] `io.wavebeans.lib.io.FileEncoderFn`
  - **File**: `lib/src/jvmTest/kotlin/io/wavebeans/lib/io/FunctionStreamOutputSpec.kt`
  - **Todo**: Migrated to a simple class with `invoke` operator.

- [x] `io.wavebeans.tests.StoreToMemoryFn`
  - **File**: `tests/src/main/kotlin/io/wavebeans/tests/StreamUtils.kt`
  - **Todo**: Migrated to a simple class with `invoke` operator.

- [x] `InputFn` (in `ScriptRunnerSpec`)
  - **File**: `cli/src/test/kotlin/io/wavebeans/cli/script/ScriptRunnerSpec.kt`
  - **Todo**: Migrate to lambda.

- [ ] `io.wavebeans.tests.MultiPartitionCorrectnessSpec` (Anonymous Fn)
  - **File**: `tests/src/test/kotlin/io/wavebeans/tests/MultiPartitionCorrectnessSpec.kt`
  - **Todo**: Migrate to lambda.

#### Documentation & Examples

- [x] `TriangularFn`
  - **File**: `docs/user/api/operations/map-window-function.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.

- [x] `ChangeAmplitudeFn`
  - **File**: `docs/user/api/functions.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.

- [x] `SignFn`
  - **File**: `docs/user/api/operations/map-operation.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.

- [x] `SumSamplesSafeFn`
  - **File**: `docs/user/api/operations/merge-operation.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.

- [x] `CsvFn`
  - **File**: `docs/user/api/outputs/csv-outputs.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.

- [x] `SequenceDetectFn`
  - **File**: `docs/user/api/outputs/wav-output.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.

- [x] `InputFn`
  - **File**: `docs/user/api/inputs/function-as-input.md`
  - **Todo**: Updated example to use lambda and simple class with `invoke`.
