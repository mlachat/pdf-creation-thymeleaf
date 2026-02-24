# Java Best Practices — Modern Java 21 Idioms

A living reference for conventions used across this project.

---

## 1. `var` (Local Variable Type Inference)

Use `var` when the right-hand side makes the type unambiguous:

```java
var list = new ArrayList<String>();       // clearly ArrayList<String>
var stream = Files.newInputStream(path);  // clearly InputStream
var map = Map.of("key", "value");         // clearly Map<String, String>
```

**Avoid `var` when:**
- The RHS is a method call with a non-obvious return type: `var result = service.process(input);`
- Complex generics need to stay visible: `Map<String, List<Map<String, Object>>>`
- Mixed-type factory methods need an explicit target type: `Map<String, Object> model = Map.ofEntries(...)`

---

## 2. Streams vs Loops

**Use streams** for stateless transform → filter → collect pipelines:

```java
var names = users.stream()
        .filter(User::isActive)
        .map(User::name)
        .toList();
```

**Use loops** for:
- Side effects (logging, I/O inside the loop body)
- Early exit (`break`, `return`)
- Complex mutable accumulation that doesn't fit a collector

---

## 3. `Optional`

Use `Optional` as a **return type only** — never as a field, parameter, or collection element.

```java
public Optional<User> findById(long id) { ... }

// Consuming
String name = findById(42)
        .map(User::name)
        .orElse("unknown");
```

**Avoid:** `Optional.get()` without checking — prefer `orElse`, `orElseThrow`, `ifPresent`, or `map`.

---

## 4. Map / List / Set Factory Methods

| Method              | Max entries | Null values | Mutability |
|---------------------|-------------|-------------|------------|
| `Map.of(k, v, ...)` | 10          | No          | Immutable  |
| `Map.ofEntries(...)` | Unlimited   | No          | Immutable  |
| `List.of(...)`       | Unlimited   | No          | Immutable  |
| `Set.of(...)`        | Unlimited   | No          | Immutable  |

**Null-value caveat:** `Map.of()` and `Map.ofEntries()` throw `NullPointerException` on null keys or values. If a null value is required (e.g., testing null-handling), use `HashMap`:

```java
// HashMap required: Map.ofEntries() rejects null values
var model = new HashMap<String, Object>();
model.put("key", null);
```

---

## 5. Text Blocks

Use text blocks for multi-line strings (HTML, SQL, JSON):

```java
var html = """
        <html>
        <body>
            <p>Hello, World!</p>
        </body>
        </html>
        """;
```

**Indentation rules:**
- The closing `"""` position determines the base indentation — all common leading whitespace up to that column is stripped.
- Use `\` at end of line to suppress the newline (line continuation).

---

## 6. Records

Use records for immutable data carriers with no business logic:

```java
public record Address(String street, String city, String zip) {}
```

**When NOT to use records:**
- Classes with mutable state
- Classes performing I/O or holding resources (e.g., `PdfGenerator`, `FreemarkerRenderer`)
- When you need inheritance (records are implicitly `final`)

---

## 7. Pattern Matching

### `instanceof` patterns

```java
if (obj instanceof String s) {
    System.out.println(s.length());
}
```

### Switch expressions

```java
String label = switch (status) {
    case ACTIVE -> "Active";
    case INACTIVE -> "Inactive";
    case PENDING -> "Pending";
};
```

### Guarded patterns (Java 21)

```java
switch (shape) {
    case Circle c when c.radius() > 10 -> "large circle";
    case Circle c -> "small circle";
    case Rectangle r -> "rectangle";
    default -> "unknown";
}
```

---

## 8. Functional Interfaces

Use the standard `java.util.function` types:

| Interface      | Signature            | Use case                    |
|----------------|----------------------|-----------------------------|
| `Function<T,R>` | `T → R`             | Transform / map             |
| `Supplier<T>`   | `() → T`            | Lazy creation / factory     |
| `Consumer<T>`   | `T → void`          | Side-effect (logging, I/O)  |
| `Predicate<T>`  | `T → boolean`       | Filter / test               |
| `UnaryOperator<T>` | `T → T`          | In-place transform          |

Prefer method references over lambdas when possible: `User::name` over `u -> u.name()`.

---

## 9. Default Interface Methods

Use default methods to evolve interfaces without breaking implementations:

```java
public interface Renderer {
    String render(String template, Map<String, Object> model);

    default String renderToFragment(String template, Map<String, Object> model) {
        return render(template, model).replaceAll("(?s)<html.*?>|</html>", "");
    }
}
```

**Prefer abstract classes** when you need:
- Constructor logic or fields
- Non-public methods
- State management

---

## 10. Immutability

Prefer immutable data structures:

```java
// Immutable collections
var names = List.of("Alice", "Bob", "Charlie");
var config = Map.of("timeout", "30", "retries", "3");

// Immutable from stream
var sorted = names.stream().sorted().toList(); // returns unmodifiable list
```

**Guidelines:**
- Declare fields `final` wherever possible
- Return defensive copies from getters if the field is a mutable collection
- Use `List.copyOf()` / `Map.copyOf()` to create immutable snapshots of mutable collections

---

## 11. AssertJ Assertions

Prefer AssertJ over JUnit assertions for better readability and failure messages.

```java
import static org.assertj.core.api.Assertions.*;

// Instead of assertTrue(list.contains("foo"))
assertThat(list).contains("foo");

// Instead of assertEquals(3, list.size())
assertThat(list).hasSize(3);

// Instead of assertThrows(NPE.class, () -> ...)
assertThatThrownBy(() -> service.call(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("must not be null");

// Chaining
assertThat(text)
        .contains("expected")
        .doesNotContain("forbidden")
        .startsWith("prefix");

// Descriptive context with as()
assertThat(pdf).as("PDF must start with magic bytes").startsWith(pdfMagicBytes);

// Byte array assertions
assertThat(bytes).isNotEmpty().startsWith(new byte[]{0x25, 0x50, 0x44, 0x46}); // %PDF
```
