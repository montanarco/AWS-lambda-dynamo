package dynamohandler;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Handler for requests to Lambda function.
 * Routes HTTP requests to appropriate handlers and delegates business logic to services.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson gson = new Gson();
    private static final String TABLE_NAME = System.getenv("DYNAMODB_TABLE");
    private ProductService productService;

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        context.getLogger().log("Received request: " + input.getHttpMethod() + " " + input.getPath());

        // Initialize ProductService with current context
        productService = new ProductService(TABLE_NAME, context);

        try {
            String httpMethod = input.getHttpMethod();

            if ("POST".equals(httpMethod)) {
                return handlePostRequest(input, context);
            } else if ("GET".equals(httpMethod)) {
                return handleGetRequest(input, context);
            } else if ("PUT".equals(httpMethod)) {
                return handlePutRequest(input, context);
            }

            return buildResponse(400, "Unsupported HTTP method: " + httpMethod);
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            return buildResponse(500, "Internal Server Error: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handlePostRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            JsonObject jsonBody = gson.fromJson(input.getBody(), JsonObject.class);
            Map<String, Object> result = productService.createProduct(jsonBody);
            return buildResponse((int) result.get("statusCode"), (String) result.get("message"));
        } catch (Exception e) {
            context.getLogger().log("Error in POST request: " + e.getMessage());
            return buildResponse(500, "Error creating product: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent handleGetRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        // TODO: Implement GET logic to retrieve products from DynamoDB
        return buildResponse(200, "GET request received");
    }

    private APIGatewayProxyResponseEvent handlePutRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        try {
            JsonObject jsonBody = gson.fromJson(input.getBody(), JsonObject.class);
            Map<String, Object> result = productService.updateProduct(jsonBody);
            return buildResponse((int) result.get("statusCode"), (String) result.get("message"));
        } catch (Exception e) {
            context.getLogger().log("Error in PUT request: " + e.getMessage());
            return buildResponse(500, "Error updating product: " + e.getMessage());
        }

    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String body) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(headers);
        response.setBody(gson.toJson(new ResponseBody(statusCode, body)));

        return response;
    }

    private static class ResponseBody {
        public int statusCode;
        public String message;

        ResponseBody(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }
    }

}
