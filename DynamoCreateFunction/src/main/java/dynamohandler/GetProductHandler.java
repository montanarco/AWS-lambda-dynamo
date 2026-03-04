package dynamohandler;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

public class GetProductHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ProductService productService = new ProductService(System.getenv("DYNAMODB_TABLE"));

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        try {
            String productId = input.getQueryStringParameters() != null
                    ? input.getQueryStringParameters().get("id")
                    : null;

            Map<String, Object> result = productService.getProduct(productId, context);
            return ResponseBuilder.build((int) result.get("statusCode"), (String) result.get("message"));
        } catch (Exception e) {
            context.getLogger().log("Error in GET /products: " + e.getMessage());
            return ResponseBuilder.build(500, "Error retrieving product: " + e.getMessage());
        }
    }
}
