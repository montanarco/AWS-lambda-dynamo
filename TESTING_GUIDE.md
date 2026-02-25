# Testing the DynamoDB Update Fix

## Overview

The `handlePutRequest` method now properly updates items in DynamoDB by their ID, preserving all other attributes.

---

## How to Test

### Using cURL

#### Test 1: Update Name Only
```bash
curl -X PUT http://localhost:3000/product \
  -H "Content-Type: application/json" \
  -d '{
    "id": "PROD123",
    "name": "Updated Product Name"
  }'
```

**Expected Response**:
```json
{
  "statusCode": 200,
  "message": "Product with ID PROD123 updated successfully"
}
```

**DynamoDB Result**: Name is updated, price/description/picture_url remain unchanged ✅

---

#### Test 2: Update Multiple Attributes
```bash
curl -X PUT http://localhost:3000/product \
  -H "Content-Type: application/json" \
  -d '{
    "id": "PROD123",
    "name": "New Name",
    "price": "99.99",
    "picture_url": "https://example.com/image.jpg"
  }'
```

**Expected Response**:
```json
{
  "statusCode": 200,
  "message": "Product with ID PROD123 updated successfully"
}
```

**DynamoDB Result**: name, price, picture_url updated; description preserved ✅

---

#### Test 3: Error Case - No Attributes to Update
```bash
curl -X PUT http://localhost:3000/product \
  -H "Content-Type: application/json" \
  -d '{
    "id": "PROD123"
  }'
```

**Expected Response**:
```json
{
  "statusCode": 400,
  "message": "No attributes to update"
}
```

---

### Using AWS CLI

#### First, create a test item in DynamoDB:
```bash
aws dynamodb put-item \
  --table-name your-table-name \
  --item '{
    "id": {"S": "PROD999"},
    "name": {"S": "Original Name"},
    "price": {"N": "29.99"},
    "description": {"S": "Original Description"},
    "picture_url": {"S": "original-url.jpg"}
  }'
```

#### View the original item:
```bash
aws dynamodb get-item \
  --table-name your-table-name \
  --key '{"id": {"S": "PROD999"}}'
```

**Output**:
```json
{
  "Item": {
    "id": {"S": "PROD999"},
    "name": {"S": "Original Name"},
    "price": {"N": "29.99"},
    "description": {"S": "Original Description"},
    "picture_url": {"S": "original-url.jpg"}
  }
}
```

#### Update only the name via your Lambda:
```bash
# Call your Lambda endpoint with:
{
  "id": "PROD999",
  "name": "Updated Name"
}
```

#### Verify the update:
```bash
aws dynamodb get-item \
  --table-name your-table-name \
  --key '{"id": {"S": "PROD999"}}'
```

**Output** (name updated, others preserved):
```json
{
  "Item": {
    "id": {"S": "PROD999"},
    "name": {"S": "Updated Name"},        ← UPDATED
    "price": {"N": "29.99"},              ← PRESERVED
    "description": {"S": "Original Description"},  ← PRESERVED
    "picture_url": {"S": "original-url.jpg"}       ← PRESERVED
  }
}
```

✅ **Success**: Only the name was updated!

---

## Integration Test Script

Save as `test-update.sh`:

```bash
#!/bin/bash

# Configuration
LAMBDA_URL="http://localhost:3000/product"  # Change to your Lambda URL
TABLE_NAME="your-table-name"

echo "Testing DynamoDB Update Fix..."
echo ""

# Test 1: Update name only
echo "Test 1: Update name only"
curl -X PUT "$LAMBDA_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEST001",
    "name": "New Name"
  }' | jq .
echo ""

# Test 2: Update multiple fields
echo "Test 2: Update multiple fields"
curl -X PUT "$LAMBDA_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEST001",
    "name": "Another Name",
    "price": "49.99",
    "description": "Updated Description"
  }' | jq .
echo ""

# Test 3: Error case - no fields
echo "Test 3: Error case - no fields to update"
curl -X PUT "$LAMBDA_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEST001"
  }' | jq .
echo ""

# Test 4: Update picture_url only
echo "Test 4: Update picture_url only"
curl -X PUT "$LAMBDA_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEST001",
    "picture_url": "https://example.com/new-image.jpg"
  }' | jq .
echo ""

echo "All tests complete!"
```

Make executable and run:
```bash
chmod +x test-update.sh
./test-update.sh
```

---

## What Gets Updated vs. What Gets Preserved

### Request Attributes Processed:
- ✅ `id` - Used as key (not updated)
- ✅ `name` - Updated if present
- ✅ `price` - Updated if present
- ✅ `description` - Updated if present
- ✅ `picture_url` - Updated if present

### Other Attributes in Item:
- ✅ `created_at` - PRESERVED
- ✅ `updated_at` - PRESERVED
- ✅ `quantity` - PRESERVED
- ✅ Any custom attributes - PRESERVED

---

## Logs to Monitor

Check your Lambda logs for these messages:

```
✅ Success: "Item with ID [ID] successfully updated in DynamoDB"
✅ Details: "Updated attributes: {name=Product A, price=29.99}"
❌ Error: "Error in PUT request: [error message]"
```

---

## Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| `"No attributes to update"` | Only ID in request | Add at least one of: name, price, description, picture_url |
| `"Item not found"` | ID doesn't exist | Item will be created with provided attributes |
| `ValidationException` | Invalid attribute value | Ensure price is numeric string, others are strings |
| `AccessDenied` | No DynamoDB permissions | Check IAM role for Lambda |

---

## Success Criteria ✅

Your update fix is working correctly when:

1. ✅ Updating a name preserves price, description, picture_url
2. ✅ Updating price doesn't affect other attributes
3. ✅ Multiple attributes can be updated in one request
4. ✅ Requesting update with no attributes returns error 400
5. ✅ All logs show "successfully updated"
6. ✅ DynamoDB confirms all attributes preserved

---

## Compile and Test

```bash
# From project root directory
cd /Users/miguelmontanez/EPAM/Java\ Global\ Learning\ Journey/AWS-Lambdas/CreateProductDynamo

# Compile
mvn clean compile

# Package
mvn package

# Run tests
mvn test
```

---

## Deployment

Once tested locally:

```bash
# Build with SAM
sam build

# Deploy to AWS
sam deploy --guided
```

Your Lambda is now ready to properly update DynamoDB items! 🚀

