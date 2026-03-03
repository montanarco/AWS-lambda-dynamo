# CreateProductDynamo — Deployment Documentation

## Overview

REST API for managing products, built with AWS Lambda, API Gateway, and DynamoDB.
Deployed via AWS SAM to the `us-east-1` region.

---

## Deployment Details

| Property         | Value                          |
|------------------|--------------------------------|
| Stack Name       | `CreateProductDynamo`          |
| Region           | `us-east-1`                    |
| AWS Account      | `108782074849`                 |
| Deployment Date  | 2026-03-03                     |
| Runtime          | Java 17                        |
| Architecture     | x86_64                         |

---

## Infrastructure Resources

| Resource         | Name / ID                                                                 |
|------------------|---------------------------------------------------------------------------|
| DynamoDB Table   | `ProductsCRUD`                                                            |
| Lambda Function  | `CreateProductFunction`                                                   |
| Lambda Function  | `GetProductFunction`                                                      |
| Lambda Function  | `UpdateProductFunction`                                                   |
| API Gateway      | `ServerlessRestApi` (Prod stage)                                          |

---

## Base URL

```
https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/
```

---

## API Endpoints

### GET /products — Retrieve a product by ID

Returns a single product matching the given `id`.

**Request**

```
GET /Prod/products/?id={productId}
```

| Parameter | Type   | Location      | Required | Description        |
|-----------|--------|---------------|----------|--------------------|
| `id`      | String | Query string  | Yes      | The product ID     |

**Example**

```bash
curl "https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/?id=abc-123"
```

**Responses**

| Status | Description                          |
|--------|--------------------------------------|
| 200    | Product found — returns product data |
| 400    | Missing `id` query parameter         |
| 404    | No product found with that ID        |
| 500    | Internal server error                |

---

### POST /products — Create a product

Creates a new product in the database.

**Request**

```
POST /Prod/products/
Content-Type: application/json
```

**Body**

```json
{
  "id": "abc-123",
  "name": "Product Name",
  "price": 29.99,
  "description": "Optional description",
  "picture_url": "https://example.com/image.jpg"
}
```

| Field         | Type   | Required | Description              |
|---------------|--------|----------|--------------------------|
| `id`          | String | Yes      | Unique product ID        |
| `name`        | String | Yes      | Product name             |
| `price`       | Number | Yes      | Product price            |
| `description` | String | No       | Product description      |
| `picture_url` | String | No       | URL of the product image |

**Example**

```bash
curl -X POST "https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/" \
  -H "Content-Type: application/json" \
  -d '{"id":"abc-123","name":"Laptop","price":999.99,"description":"High-end laptop"}'
```

**Responses**

| Status | Description                  |
|--------|------------------------------|
| 200    | Product created successfully |
| 500    | Internal server error        |

---

### PUT /products — Update a product

Updates one or more fields of an existing product.

**Request**

```
PUT /Prod/products/
Content-Type: application/json
```

**Body**

```json
{
  "id": "abc-123",
  "name": "Updated Name",
  "price": 899.99,
  "description": "Updated description",
  "picture_url": "https://example.com/new-image.jpg"
}
```

| Field         | Type   | Required | Description                          |
|---------------|--------|----------|--------------------------------------|
| `id`          | String | Yes      | ID of the product to update          |
| `name`        | String | No       | New product name                     |
| `price`       | Number | No       | New product price                    |
| `description` | String | No       | New product description              |
| `picture_url` | String | No       | New product image URL                |

At least one optional field must be provided.

**Example**

```bash
curl -X PUT "https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/" \
  -H "Content-Type: application/json" \
  -d '{"id":"abc-123","price":849.99}'
```

**Responses**

| Status | Description                         |
|--------|-------------------------------------|
| 200    | Product updated successfully        |
| 400    | No updatable fields provided        |
| 500    | Internal server error               |

---

## Response Format

All endpoints return JSON in the following structure:

```json
{
  "statusCode": 200,
  "message": "..."
}
```

---

## Quick Test Commands

```bash
# Create a product
curl -X POST "https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/" \
  -H "Content-Type: application/json" \
  -d '{"id":"p001","name":"Laptop","price":999.99}'

# Get a product
curl "https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/?id=p001"

# Update a product
curl -X PUT "https://kqa62tgqv6.execute-api.us-east-1.amazonaws.com/Prod/products/" \
  -H "Content-Type: application/json" \
  -d '{"id":"p001","price":849.99}'
```
