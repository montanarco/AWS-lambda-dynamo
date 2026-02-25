# Visual Comparison: The DynamoDB Update Fix

## 🔄 REQUEST FLOW COMPARISON

### ❌ BEFORE (PutItemRequest - WRONG)
```
User Request
    ↓
PUT /product
{
  "id": "P001",
  "name": "New Name"
}
    ↓
DynamoDbClient.putItem(request)
    ↓
DynamoDB receives: "Replace item P001 with this data"
    ↓
DynamoDB Action:
  ┌─────────────────────────────────────────────┐
  │ DELETE everything for item P001             │
  │ INSERT new item with ONLY:                  │
  │   - id: P001                                │
  │   - name: New Name                          │
  └─────────────────────────────────────────────┘
    ↓
Result in DynamoDB:
{
  "id": "P001",
  "name": "New Name"
}
    ↓
❌ LOST: price, description, picture_url, created_at, etc.
```

### ✅ AFTER (UpdateItemRequest - CORRECT)
```
User Request
    ↓
PUT /product
{
  "id": "P001",
  "name": "New Name"
}
    ↓
DynamoDbClient.updateItem(request)
    ↓
DynamoDB receives: "Update item P001 SET name = 'New Name'"
    ↓
DynamoDB Action:
  ┌─────────────────────────────────────────────┐
  │ FIND item where id = P001                   │
  │ UPDATE ONLY these fields:                   │
  │   - name: New Name                          │
  │ PRESERVE everything else:                   │
  │   - price, description, picture_url, etc.   │
  └─────────────────────────────────────────────┘
    ↓
Result in DynamoDB:
{
  "id": "P001",
  "name": "New Name",           ← UPDATED
  "price": 99.99,               ← PRESERVED
  "description": "Product...",  ← PRESERVED
  "picture_url": "url.jpg",     ← PRESERVED
  "created_at": "2026-02-20"    ← PRESERVED
}
    ↓
✅ NOTHING LOST - ALL DATA SAFE
```

---

## 📊 ATTRIBUTE PRESERVATION MATRIX

### Example Product Before Update:
```
{
  "id": "PROD-123",
  "name": "Original Name",
  "price": "49.99",
  "description": "Original Description",
  "picture_url": "original-url.jpg",
  "created_at": "2026-01-15",
  "updated_at": "2026-02-20",
  "stock": "100",
  "category": "Electronics"
}
```

### Update Request:
```json
{
  "id": "PROD-123",
  "name": "Updated Name",
  "price": "39.99"
}
```

### ❌ BEFORE (PutItemRequest - DATA LOSS):
```
{
  "id": "PROD-123",
  "name": "Updated Name"
}
```
| Field | Result |
|-------|--------|
| id | ✅ Kept (in request) |
| name | ✅ Updated (in request) |
| price | ❌ **LOST** (not in request) |
| description | ❌ **LOST** (not in request) |
| picture_url | ❌ **LOST** (not in request) |
| created_at | ❌ **LOST** (not in request) |
| updated_at | ❌ **LOST** (not in request) |
| stock | ❌ **LOST** (not in request) |
| category | ❌ **LOST** (not in request) |

### ✅ AFTER (UpdateItemRequest - NO DATA LOSS):
```
{
  "id": "PROD-123",
  "name": "Updated Name",
  "price": "39.99",
  "description": "Original Description",
  "picture_url": "original-url.jpg",
  "created_at": "2026-01-15",
  "updated_at": "2026-02-20",
  "stock": "100",
  "category": "Electronics"
}
```
| Field | Result |
|-------|--------|
| id | ✅ Preserved (key) |
| name | ✅ **Updated** |
| price | ✅ **Updated** |
| description | ✅ Preserved |
| picture_url | ✅ Preserved |
| created_at | ✅ Preserved |
| updated_at | ✅ Preserved |
| stock | ✅ Preserved |
| category | ✅ Preserved |

---

## 🔑 CODE FLOW COMPARISON

### ❌ BEFORE (WRONG METHOD)
```
1. Create Map with only specified fields
   ├── id
   ├── name
   └── price

2. Build PutItemRequest
   └── .item(map)  ← Entire item to replace

3. Execute putItem()
   └── Replaces entire item with only these 3 fields

4. Result
   └── ❌ All other fields deleted
```

### ✅ AFTER (CORRECT METHOD)
```
1. Extract product ID from request
   └── "P001"

2. Build dynamic UpdateExpression
   ├── Check: does request have "name"? YES → add to expression
   ├── Check: does request have "price"? YES → add to expression
   ├── Check: does request have "description"? NO → skip
   └── Result: "SET name = :name, price = :price"

3. Build ExpressionAttributeValues
   ├── :name → value from request
   └── :price → value from request

4. Build UpdateItemRequest
   ├── .key({"id": "P001"})  ← Which item to update
   ├── .updateExpression(...)  ← What to update
   ├── .expressionAttributeValues(...)  ← New values
   └── .returnValues("ALL_NEW")  ← Return updated item

5. Execute updateItem()
   ├── Find item where id = P001
   ├── Update ONLY the specified fields
   ├── Leave all other fields unchanged
   └── Return complete updated item

6. Result
   └── ✅ Only specified fields updated, others preserved
```

---

## 📈 BEHAVIOR COMPARISON

### Scenario: 5 Product Updates

#### ❌ WITH PutItemRequest (BEFORE):
```
Update 1: name              → [LOST: price, description, picture_url, created_at]
Update 2: price, name       → [LOST: description, picture_url, created_at]
Update 3: description only  → [LOST: name, price, picture_url, created_at]
Update 4: name, price       → [LOST: description, picture_url, created_at]
Update 5: All fields        → [OK: everything provided]

Result: 4 out of 5 updates cause data loss ❌❌❌❌
```

#### ✅ WITH UpdateItemRequest (AFTER):
```
Update 1: name              → [OK: only name updated]
Update 2: price, name       → [OK: both updated]
Update 3: description only  → [OK: only description updated]
Update 4: name, price       → [OK: both updated]
Update 5: All fields        → [OK: all updated]

Result: 5 out of 5 updates are safe ✅✅✅✅✅
```

---

## 🎯 DECISION TREE

### When to use each method:

```
Do you want to update specific fields in an item?
├─ YES → Use UpdateItemRequest ✅
│        (Updates only specified fields)
│
└─ NO, I want to replace the entire item
   └─ Use PutItemRequest ✅
      (Replaces everything)
```

### For this project:

```
Update Product endpoint:
├─ Receives: {"id": "P1", "name": "New"}
├─ Want to update: Only name
├─ Need to preserve: price, description, etc.
└─ Therefore: Use UpdateItemRequest ✅
```

---

## 💻 CODE MAPPING

### Location in App.java

```
handlePutRequest() method
├─ Line 81: Method definition
├─ Line 83: Extract body and parse JSON
├─ Line 86: Extract product ID
├─ Line 89-107: Build update expression dynamically
│             (only include attributes in request)
├─ Line 109-112: Validate update expression
├─ Line 115-126: Build UpdateItemRequest
│               (correct way to update items)
├─ Line 128-130: Execute updateItem()
│               (AWS SDK call)
├─ Line 131-135: Return success response
└─ Line 137-140: Exception handling
```

---

## ✅ VERIFICATION CHECKLIST

Every requirement for proper DynamoDB updates:

- [x] Uses UpdateItemRequest (not PutItemRequest)
- [x] Specifies item key (id field)
- [x] Uses UpdateExpression (SET clause)
- [x] Includes ExpressionAttributeValues
- [x] Returns ALL_NEW (returns updated item)
- [x] Dynamic expression building (only updates provided fields)
- [x] Error handling
- [x] Logging
- [x] Validates input (no empty updates)

---

## 🎉 SUMMARY

| Aspect | Before ❌ | After ✅ |
|--------|----------|----------|
| Method | PutItemRequest | UpdateItemRequest |
| Behavior | Full replacement | Partial update |
| Data Safety | Dangerous | Safe |
| Code Correctness | Wrong approach | Best practice |
| Data Loss Risk | HIGH ❌ | NONE ✅ |
| Attribute Preservation | None ❌ | All preserved ✅ |

---

**RESULT: Your DynamoDB updates are now SAFE and CORRECT!** ✅

