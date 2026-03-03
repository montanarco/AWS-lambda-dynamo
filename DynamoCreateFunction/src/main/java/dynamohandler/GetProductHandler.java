package dynamohandler;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

public class GetProductHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson gson = new Gson();
    private static final ProductService productService = new ProductService(System.getenv("DYNAMODB_TABLE"));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String productId = input.getQueryStringParameters() != null
                    ? input.getQueryStringParameters().get("id")
                    : null;

            Map<String, Object> result = productService.getProduct(productId, context);
            return buildResponse((int) result.get("statusCode"), (String) result.get("message"));
        } catch (Exception e) {
            context.getLogger().log("Error in GET /products: " + e.getMessage());
            return buildResponse(500, "Error retrieving product: " + e.getMessage());
        }
    }

    private APIGatewayProxyResponseEvent buildResponse(int statusCode, String message) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(headers);
        response.setBody(gson.toJson(new ResponseBody(statusCode, message)));
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
