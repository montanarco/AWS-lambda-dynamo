package dynamohandler;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.gson.JsonObject;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

/**
 * ProductService handles all product-related DynamoDB operations.
 * This service is responsible for creating, updating, and managing products.
 */
public class ProductService {

    private final String tableName;
    private final Context context;

    /**
     * Constructor for ProductService.
     *
     * @param tableName the DynamoDB table name
     * @param context   the Lambda context for logging
     */
    public ProductService(String tableName, Context context) {
        this.tableName = tableName;
        this.context = context;
    }

    /**
     * Creates a new product in DynamoDB.
     *
     * @param jsonBody the JSON body containing product information (id, name, price)
     * @return a map containing status and message information
     * @throws Exception if an error occurs during the operation
     */
    public Map<String, Object> createProduct(JsonObject jsonBody) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try (DynamoDbClient dynamoDb = DynamoDbClient.builder().build()) {
            Map<String, AttributeValue> item = new HashMap<>();

            // Add product attributes
            item.put("id", AttributeValue.builder().s(jsonBody.get("id").getAsString()).build());
            item.put("name", AttributeValue.builder().s(jsonBody.get("name").getAsString()).build());
            item.put("price", AttributeValue.builder().n(jsonBody.get("price").getAsString()).build());

            PutItemRequest request = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .build();

            PutItemResponse response = dynamoDb.putItem(request);
            context.getLogger().log("Item successfully created in DynamoDB");

            result.put("statusCode", 200);
            result.put("message", "Product created successfully");
            result.put("success", true);
        } catch (Exception e) {
            context.getLogger().log("Error in createProduct: " + e.getMessage());
            result.put("statusCode", 500);
            result.put("message", "Error creating product: " + e.getMessage());
            result.put("success", false);
            throw e;
        }

        return result;
    }

    /**
     * Updates an existing product in DynamoDB by product ID.
     * Only updates attributes that are present in the request.
     *
     * @param jsonBody the JSON body containing product ID and attributes to update
     * @return a map containing status and message information
     * @throws Exception if an error occurs during the operation
     */
    public Map<String, Object> updateProduct(JsonObject jsonBody) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try (DynamoDbClient dynamoDb = DynamoDbClient.builder().build()) {
            String productId = jsonBody.get("id").getAsString();

            // Build dynamic update expression
            StringBuilder updateExpression = new StringBuilder("SET ");
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

            // Add attributes to update if present
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

            // Validate that there are attributes to update
            if (expressionAttributeValues.isEmpty()) {
                result.put("statusCode", 400);
                result.put("message", "No attributes to update");
                result.put("success", false);
                return result;
            }

            // Remove trailing comma and space
            updateExpression.setLength(updateExpression.length() - 2);

            // Create and execute update request
            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(tableName)
                    .key(new HashMap<String, AttributeValue>() {{
                        put("id", AttributeValue.builder().s(productId).build());
                    }})
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionAttributeValues)
                    .returnValues("ALL_NEW")
                    .build();

            UpdateItemResponse response = dynamoDb.updateItem(request);
            context.getLogger().log("Item with ID " + productId + " successfully updated in DynamoDB");
            context.getLogger().log("Updated attributes: " + response.attributes().toString());

            result.put("statusCode", 200);
            result.put("message", "Product with ID " + productId + " updated successfully");
            result.put("success", true);
            result.put("attributes", response.attributes());

        } catch (Exception e) {
            context.getLogger().log("Error in updateProduct: " + e.getMessage());
            e.printStackTrace();
            result.put("statusCode", 500);
            result.put("message", "Error updating product: " + e.getMessage());
            result.put("success", false);
            throw e;
        }

        return result;
    }

}

