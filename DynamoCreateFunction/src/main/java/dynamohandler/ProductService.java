package dynamohandler;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.JsonObject;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 * ProductService handles all product-related DynamoDB operations.
 * The DynamoDbClient is static so it is initialized once per Lambda container
 * and reused across warm invocations.
 */
public class ProductService {

    private static final DynamoDbClient dynamoDb = DynamoDbClient.builder().build();
    private final String tableName;

    public ProductService(String tableName) {
        this.tableName = tableName;
    }

    public Map<String, Object> createProduct(JsonObject jsonBody, Context context) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(jsonBody.get("id").getAsString()).build());
            item.put("name", AttributeValue.builder().s(jsonBody.get("name").getAsString()).build());
            item.put("price", AttributeValue.builder().n(jsonBody.get("price").getAsString()).build());
            if (jsonBody.has("description")) {
                item.put("description", AttributeValue.builder().s(jsonBody.get("description").getAsString()).build());
            }
            if (jsonBody.has("picture_url")) {
                item.put("picture_url", AttributeValue.builder().s(jsonBody.get("picture_url").getAsString()).build());
            }

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            PutItemResponse response = dynamoDb.putItem(request);
            context.getLogger().log("Product created in DynamoDB: " + jsonBody.get("id").getAsString());

            result.put("statusCode", 200);
            result.put("message", "Product created successfully");
        } catch (Exception e) {
            context.getLogger().log("Error in createProduct: " + e.getMessage());
            result.put("statusCode", 500);
            result.put("message", "Error creating product: " + e.getMessage());
            throw e;
        }
        return result;
    }

    public Map<String, Object> getProduct(String productId, Context context) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try {
            if (productId == null || productId.isBlank()) {
                ScanResponse scanResponse = dynamoDb.scan(ScanRequest.builder().tableName(tableName).build());
                context.getLogger().log("All products retrieved, count: " + scanResponse.count());
                result.put("statusCode", 200);
                result.put("message", scanResponse.items().toString());
                return result;
            }

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("id", AttributeValue.builder().s(productId).build()))
                    .build();

            GetItemResponse response = dynamoDb.getItem(request);
            if (!response.hasItem()) {
                result.put("statusCode", 404);
                result.put("message", "Product with id " + productId + " not found");
                return result;
            }

            context.getLogger().log("Product retrieved: " + productId);
            result.put("statusCode", 200);
            result.put("message", response.item().toString());
        } catch (Exception e) {
            context.getLogger().log("Error in getProduct: " + e.getMessage());
            result.put("statusCode", 500);
            result.put("message", "Error retrieving product: " + e.getMessage());
            throw e;
        }
        return result;
    }

    public Map<String, Object> updateProduct(JsonObject jsonBody, Context context) throws Exception {
        Map<String, Object> result = new HashMap<>();
        try {
            String productId = jsonBody.get("id").getAsString();

            StringBuilder updateExpression = new StringBuilder("SET ");
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

            if (jsonBody.has("name")) {
                updateExpression.append("name = :name, ");
                expressionAttributeValues.put(":name",
                        AttributeValue.builder().s(jsonBody.get("name").getAsString()).build());
            }
            if (jsonBody.has("price")) {
                updateExpression.append("price = :price, ");
                expressionAttributeValues.put(":price",
                        AttributeValue.builder().n(jsonBody.get("price").getAsString()).build());
            }
            if (jsonBody.has("description")) {
                updateExpression.append("description = :description, ");
                expressionAttributeValues.put(":description",
                        AttributeValue.builder().s(jsonBody.get("description").getAsString()).build());
            }
            if (jsonBody.has("picture_url")) {
                updateExpression.append("picture_url = :picture_url, ");
                expressionAttributeValues.put(":picture_url",
                        AttributeValue.builder().s(jsonBody.get("picture_url").getAsString()).build());
            }

            if (expressionAttributeValues.isEmpty()) {
                result.put("statusCode", 400);
                result.put("message", "No attributes to update");
                return result;
            }

            updateExpression.setLength(updateExpression.length() - 2);

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(Map.of("id", AttributeValue.builder().s(productId).build()))
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionAttributeValues)
                    .returnValues("ALL_NEW")
                    .build();

            UpdateItemResponse response = dynamoDb.updateItem(request);
            context.getLogger().log("Product updated: " + productId);

            result.put("statusCode", 200);
            result.put("message", "Product with ID " + productId + " updated successfully");
        } catch (Exception e) {
            context.getLogger().log("Error in updateProduct: " + e.getMessage());
            result.put("statusCode", 500);
            result.put("message", "Error updating product: " + e.getMessage());
            throw e;
        }
        return result;
    }
}
