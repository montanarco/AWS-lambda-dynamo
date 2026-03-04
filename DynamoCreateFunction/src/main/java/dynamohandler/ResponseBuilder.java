package dynamohandler;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

class ResponseBuilder {

    private static final Gson gson = new Gson();

    static APIGatewayProxyResponseEvent build(int statusCode, String message) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(statusCode);
        response.setHeaders(headers);
        response.setBody(gson.toJson(new ResponseBody(statusCode, message)));
        return response;
    }

    static class ResponseBody {
        public int statusCode;
        public String message;

        ResponseBody(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }
    }
}
