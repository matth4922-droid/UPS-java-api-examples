package com.ups.api.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.openapitools.shipping.client.model.SHIPRequestWrapper;
import org.openapitools.shipping.client.model.SHIPResponseWrapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ups.api.app.tool.ShipApi;
import com.ups.api.app.tool.Util;

class ShippingOfflineTest {

	@Test
	void shipmentHappyPathThroughOauthAndShipApi() throws Exception {
		AppConfig appConfig = new AppConfig();
		appConfig.setClientID("test-client");
		appConfig.setSecret("test-secret");
		appConfig.setOauthBaseUrl("http://localhost");
		appConfig.setShippingBaseUrl("http://localhost/api/");
		appConfig.setShippingVersion("v1");
		appConfig.setTransactionSrc("testing");
		appConfig.setTokenExipryToleranceInSec(5);

		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();

		server.expect(request -> {
			assertEquals(HttpMethod.POST, request.getMethod());
			assertEquals("http://localhost/security/v1/oauth/token", request.getURI().toString());
		}).andRespond(withSuccess(
				"{\"token_type\":\"Bearer\",\"issued_at\":\"1\",\"client_id\":\"test-client\","
						+ "\"access_token\":\"test-token\",\"expires_in\":\"3600\",\"status\":\"approved\"}",
				MediaType.APPLICATION_JSON));

		server.expect(request -> {
			assertEquals(HttpMethod.POST, request.getMethod());
			assertEquals("Bearer test-token", request.getHeaders().getFirst("Authorization"));
			assertTrue(request.getURI().toString().startsWith("http://localhost/api/shipments/v1/ship"));
		}).andRespond(withSuccess(
				"{\"ShipmentResponse\":{\"Response\":{\"ResponseStatus\":{\"Code\":\"1\",\"Description\":\"Success\"}},"
						+ "\"ShipmentResults\":{\"ShipmentIdentificationNumber\":\"1Z12345E0205271688\","
						+ "\"PackageResults\":{\"TrackingNumber\":\"1Z12345E0205271688\"}}}}",
				MediaType.APPLICATION_JSON));

		SHIPRequestWrapper shipmentRequest = new SHIPRequestWrapper();
		ShipApi shipApi = new ShippingDemo(restTemplate, appConfig).getShipApi(restTemplate, appConfig);
		SHIPResponseWrapper response = Util.jsonResultPreprocess(
				shipApi.shipment(appConfig.getShippingVersion(), shipmentRequest, "test-transaction-id",
						appConfig.getTransactionSrc(), null),
				Util.getJsonToObjectConversionMap(), SHIPResponseWrapper.class);

		assertEquals("1Z12345E0205271688",
				response.getShipmentResponse().getShipmentResults().getPackageResults().get(0).getTrackingNumber());
		server.verify();
	}
}
