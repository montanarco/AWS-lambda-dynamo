# AWS Serverless Products API
### API Gateway · Lambda · DynamoDB

A serverless REST API for managing products, built with Java and deployed on AWS using the Serverless Application Model (SAM). The API supports creating, reading, and updating products stored in DynamoDB, and is consumed by a companion front-end application.

**Front-end repo:** [github.com/montanarco/AWS-lambda-dynamo-frontend](https://github.com/montanarco/AWS-lambda-dynamo-frontend)

---

## Architecture

```
Client → API Gateway → Lambda Function → DynamoDB
```

Each HTTP method routes to a dedicated Lambda handler:

| Method | Path              | Handler                  |
|--------|-------------------|--------------------------|
| POST   | `/products`       | `CreateProductHandler`   |
| GET    | `/products`       | `GetProductHandler`      |
| PUT    | `/products/{id}`  | `UpdateProductHandler`   |

---

## Technologies Used

**Backend**
- Java 17
- AWS Lambda (`aws-lambda-java-core`, `aws-lambda-java-events`)
- AWS DynamoDB (via AWS SDK v2 `DynamoDbClient`)
- AWS API Gateway (REST API, provisioned implicitly by SAM)
- AWS SAM (Serverless Application Model) for infrastructure-as-code and deployment

**Infrastructure**
- AWS CloudFormation (SAM compiles to it)
- IAM Execution Roles with least-privilege DynamoDB access (no hardcoded credentials — the SDK resolves them from the Lambda execution role at runtime)

**Build & Tooling**
- Maven
- SAM CLI
- Docker (for local Lambda emulation)

---

## Project Structure

```
.
├── template.yaml                        # SAM infrastructure definition
├── pom.xml                              # Maven build config
├── samconfig.toml.example               # SAM deploy config template
├── events/
│   └── event.json                       # Sample API Gateway event for local testing
├── DynamoCreateFunction/src/main/java/dynamohandler/
│   ├── CreateProductHandler.java
│   ├── GetProductHandler.java
│   ├── UpdateProductHandler.java
│   ├── ProductService.java              # DynamoDB field mapping logic
│   └── ResponseBuilder.java
└── tech docs/
    ├── deployment.md                    # API reference with placeholder URLs
    └── openapi.yaml                     # OpenAPI spec
```

---

## Running Locally

### Prerequisites

- [Java 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
- [Maven](https://maven.apache.org/install.html)
- [SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
- [Docker](https://hub.docker.com/search/?type=edition&offering=community) (required by SAM for local emulation)

### 1. Build

```bash
sam build
```

### 2. Start the local API

```bash
sam local start-api
```

The API will be available at `http://localhost:3000`.

### 3. Test the endpoints

```bash
# Create a product
curl -X POST http://localhost:3000/products \
  -H "Content-Type: application/json" \
  -d '{"id":"p001","name":"Laptop","price":999.99,"description":"High-end laptop"}'

# Get a product
curl "http://localhost:3000/products?id=p001"

# Update a product
curl -X PUT http://localhost:3000/products \
  -H "Content-Type: application/json" \
  -d '{"id":"p001","price":849.99}'
```

### 4. Invoke a function directly (optional)

```bash
sam local invoke CreateProductFunction --event events/event.json
```

---

## Deploying to AWS

Copy the example config and fill in your values:

```bash
cp samconfig.toml.example samconfig.toml
```

Then deploy:

```bash
sam build && sam deploy
```

On first deploy, use `--guided` to walk through the configuration prompts:

```bash
sam build && sam deploy --guided
```

---

## Data Model

| Field         | Type   | Required | Description              |
|---------------|--------|----------|--------------------------|
| `id`          | String | Yes      | Unique product ID (PK)   |
| `name`        | String | Yes      | Product name             |
| `price`       | Number | Yes      | Product price            |
| `description` | String | No       | Product description      |
| `picture_url` | String | No       | URL of the product image |

---

## Front-end

The companion front-end application consumes this API through API Gateway. It provides a product list view with create and edit capabilities, and refreshes automatically after each operation.

> The front-end UI was initially generated with **Google Gemini** and later restructured and refined with **Claude Code**.

---

## Teardown

To remove all deployed AWS resources:

```bash
sam delete --stack-name CreateProductDynamo
```
