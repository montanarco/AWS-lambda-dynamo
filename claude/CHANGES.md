# Changes Summary

## 1. `template.yaml` — Infrastructure Fixes

| Issue | Fix |
|---|---|
| `CodeUri: DynamoProductsFunction/` (folder did not exist) | Changed to `DynamoCreateFunction/` |
| `TableName: usersDB` | Changed to `Products` to match the actual DynamoDB table |
| `AttributeType: N` for `id` | Changed to `S` (String) — the payload sends `"id": "2"` as a string |
| Missing `DYNAMODB_TABLE` env var | Added `DYNAMODB_TABLE: !Ref ProductsTable` (what `App.java` was reading via `System.getenv`) |
| Wrong resource references (`!Ref Products`, `!GetAtt Products.Arn`) | Fixed to `!Ref ProductsTable` / `!GetAtt ProductsTable.Arn` |
| Only `GET` wired to API Gateway | Added `POST` and `PUT` events for `/products` |
| Wrong output URL (`/hello/`) | Fixed to `/products/` |
| Repeated config across functions | Moved shared settings (runtime, CodeUri, memory, env, policies) to `Globals` |

---

## 2. `ProductService.java` — Fixes & Refactoring

### Bug fix: missing fields in `createProduct()`
The original `createProduct()` only saved `id`, `name`, and `price` to DynamoDB.
`description` and `picture_url` — both present in the request payload — were silently dropped.

**Fix:** added `description` and `picture_url` to the `PutItemRequest`.

### Performance: static `DynamoDbClient`
The original code created a new `DynamoDbClient` on every Lambda invocation inside a `try-with-resources` block.
This is expensive — the SDK client initializes credential providers, an HTTP client, and regional endpoints on every construction.

**Fix:** `DynamoDbClient` is now a `static final` field, initialized once when the Lambda container starts and reused across all warm invocations.

```java
// Before — new client on every request
try (DynamoDbClient dynamoDb = DynamoDbClient.builder().build()) { ... }

// After — one client per container lifetime
private static final DynamoDbClient dynamoDb = DynamoDbClient.builder().build();
```

### `Context` moved from constructor to method parameters
Since `Context` is a per-invocation object and `ProductService` is now instantiated once (static), it can no longer be stored as an instance field. It is passed directly to each method that needs it for logging.

### `getProduct()` implemented
A real `GetItem` implementation was added to support the GET handler. It accepts a product ID and returns the item from DynamoDB, or a 404 if not found.

---

## 3. Handler Restructuring — One Lambda per HTTP verb

### Before
A single `App.java` handler received all requests and routed by HTTP method:

```
API Gateway → App.handleRequest() → if POST / if GET / if PUT → ProductService
```

Every request paid the cost of method routing and loading all possible code paths.

### After
Three dedicated handlers, each wired directly by API Gateway:

```
POST /products → CreateProductHandler → ProductService.createProduct()
GET  /products → GetProductHandler    → ProductService.getProduct()
PUT  /products → UpdateProductHandler → ProductService.updateProduct()
```

**Benefits:**
- No routing logic executed at runtime
- Each handler only loads the code it needs
- `ProductService` is a `static final` field in each handler — instantiated once per container
- Functions can be independently tuned (memory, timeout, concurrency) as usage patterns differ
- `App.java` deleted — no dead code

---

## 4. `Producto.java` — Broken Annotations Removed

The original file mixed AWS SDK v1 annotations (`@DynamoDBTable`, `@DynamoDBHashKey`) with SDK v2 annotations (`@Dynamodb.Attribute`), which would cause a compilation error. It was also unused by `ProductService`.

**Fix:** replaced with a clean plain Java POJO (getters/setters, no framework annotations).

---

## File Changes at a Glance

| File | Action |
|---|---|
| `template.yaml` | Updated — infrastructure fixes + 3 separate functions |
| `ProductService.java` | Updated — static client, full field support, `getProduct()` added |
| `Producto.java` | Updated — removed broken mixed SDK annotations, now a plain POJO |
| `CreateProductHandler.java` | Created — handles `POST /products` |
| `GetProductHandler.java` | Created — handles `GET /products` |
| `UpdateProductHandler.java` | Created — handles `PUT /products` |
| `App.java` | Deleted — replaced by dedicated handlers |
