# Code Review Guidelines — Java Best Practices

Actionable rules for all PDF-generation modules in this project.

---

## Exception Handling

- **No bare `RuntimeException`.**
  Use `IllegalStateException` for broken invariants (e.g. missing classpath resource)
  and `UncheckedIOException` for I/O failures.
- **Catch specific types** — never `catch (Exception e)`.
- **Preserve the cause chain** — always pass the original exception as the cause.

## Resource Management

- All `Closeable` / `AutoCloseable` resources **must** use try-with-resources.
- `deleteOnExit()` is acceptable for short-lived CLI tools but leaks in long-running
  servers — document the tradeoff with a comment when used.

## Input Validation

- Call `Objects.requireNonNull(param, "param must not be null")` at the top of every
  public method for each reference parameter.

## Constructor Design

- Document I/O side effects (temp file creation, classpath scanning) in the class-level Javadoc.
- Name helper methods accurately — avoid misleading "once" semantics when the method
  runs on every construction.

## Logging

- Every production class must declare an SLF4J logger:
  ```java
  private static final Logger log = LoggerFactory.getLogger(MyClass.class);
  ```
- Log `warn` / `error` **before** rethrowing an exception.
- Log `debug` at the start and end of significant operations (PDF generation, template rendering).

## Documentation

- Every public class must have class-level Javadoc describing its purpose.
- Every public method must have method-level Javadoc with `@param`, `@return`, and `@throws` tags.

## Constants

- Extract magic numbers and resource paths into `private static final` named constants.
- Examples: buffer sizes, font paths, template directories.

## Testing

- Every production class must have a corresponding test class.
- Extract shared test helpers into a `TestHelper` utility to avoid duplication.
- Include **error-path tests** (null inputs, missing resources) alongside happy-path tests.
