package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class DemoApplication {

    public static class CrptApi {
        private final int requestLimit;
        private final long timeIntervalMillis;
        private int requestCount;
        private long startTime;
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        public CrptApi(TimeUnit timeUnit, int requestLimit) {
            this.requestLimit = requestLimit;
            this.timeIntervalMillis = timeUnit.toMillis(1);
            this.requestCount = 0;
            this.startTime = System.currentTimeMillis();
            this.httpClient = HttpClient.newHttpClient();
            this.objectMapper = new ObjectMapper();
        }

        public synchronized void putDocumentIntoCirculation(Document document, String signature) throws InterruptedException, IOException {
            long currentTime = System.currentTimeMillis();

            if (currentTime - startTime > timeIntervalMillis) {
                startTime = currentTime;
                requestCount = 0;
            }

            while (requestCount >= requestLimit) {
                long sleepTime = timeIntervalMillis - (currentTime - startTime);
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }
                startTime = System.currentTimeMillis();
                requestCount = 0;
                currentTime = System.currentTimeMillis();
            }

            requestCount++;
            executeRequest(document, signature);
        }

        private void executeRequest(Document document, String signature) throws IOException, InterruptedException {
            String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
            String jsonBody = createRequestBody(document, signature);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Failed to execute request: " + response.body());
            }

            System.out.println("Document submitted successfully: " + response.body());
        }

        private String createRequestBody(Document document, String signature) throws IOException {
            ObjectNode jsonObject = objectMapper.createObjectNode();

            jsonObject.put("doc_id", document.getDocId());
            jsonObject.put("doc_status", document.getDocStatus());
            jsonObject.put("doc_type", document.getDocType());
            jsonObject.put("importRequest", document.isImportRequest());
            jsonObject.put("owner_inn", document.getOwnerInn());
            jsonObject.put("participant_inn", document.getParticipantInn());
            jsonObject.put("producer_inn", document.getProducerInn());
            jsonObject.put("production_date", document.getProductionDate());
            jsonObject.put("production_type", document.getProductionType());
            jsonObject.put("reg_date", document.getRegDate());
            jsonObject.put("reg_number", document.getRegNumber());

            ObjectNode descriptionNode = jsonObject.putObject("description");
            descriptionNode.put("participantInn", document.getDescription().getParticipantInn());

            ObjectNode productNode = objectMapper.createObjectNode();
            Product product = document.getProduct();

            productNode.put("certificate_document", product.getCertificateDocument());
            productNode.put("certificate_document_date", product.getCertificateDocumentDate());
            productNode.put("certificate_document_number", product.getCertificateDocumentNumber());
            productNode.put("owner_inn", product.getOwnerInn());
            productNode.put("producer_inn", product.getProducerInn());
            productNode.put("production_date", product.getProductionDate());
            productNode.put("tnved_code", product.getTnvedCode());
            productNode.put("uit_code", product.getUitCode());
            productNode.put("uitu_code", product.getUituCode());

            jsonObject.putArray("products").add(productNode);
            jsonObject.put("signature", signature);

            return objectMapper.writeValueAsString(jsonObject);
        }

        public static void main(String[] args) throws InterruptedException, IOException {
            CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

            Document doc = new Document("123", "ACTIVE", "LP_INTRODUCE_GOODS", true, "123456789", "987654321", "123456789",
                    "2020-01-23", "TYPE", "2020-01-23", "REG123", new Description("987654321"),
                    new Product("DOC123", "2020-01-23", "NUM123", "123456789", "987654321",
                            "2020-01-23", "CODE", "UIT123", "UITU123"));

            String signature = "sample-signature";

            for (int i = 0; i < 10; i++) {
                api.putDocumentIntoCirculation(doc, signature);
            }
        }

        @Getter
        @Setter
        @AllArgsConstructor
        @NoArgsConstructor
        static class Document {
            private String docId;
            private String docStatus;
            private String docType;
            private boolean importRequest;
            private String ownerInn;
            private String participantInn;
            private String producerInn;
            private String productionDate;
            private String productionType;
            private String regDate;
            private String regNumber;
            private Description description;
            private Product product;
        }

        @Getter
        @AllArgsConstructor
        static class Description {
            private String participantInn;
        }

        @Getter
        @NoArgsConstructor
        @Setter
        static class Product {
            private String certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private String ownerInn;
            private String producerInn;
            private String productionDate;
            private String tnvedCode;
            private String uitCode;
            private String uituCode;

            public Product(String certificateDocument, String certificateDocumentDate, String certificateDocumentNumber,
                           String ownerInn, String producerInn, String productionDate, String tnvedCode, String uitCode,
                           String uituCode) {
                this.certificateDocument = certificateDocument;
                this.certificateDocumentDate = certificateDocumentDate;
                this.certificateDocumentNumber = certificateDocumentNumber;
                this.ownerInn = ownerInn;
                this.producerInn = producerInn;
                this.productionDate = productionDate;
                this.tnvedCode = tnvedCode;
                this.uitCode = uitCode;
                this.uituCode = uituCode;
            }
        }
    }
}
