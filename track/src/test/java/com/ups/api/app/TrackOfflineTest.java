package com.ups.api.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.track.client.model.TrackApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.ups.api.TrackApplication;

@SpringBootTest(classes = TrackApplication.class, properties = {
		"api.oauth.partner.client.id=test-client",
		"api.oauth.partner.secret=test-secret",
		"api.oauth.base.url=http://localhost/",
		"api.track.base.url=http://localhost/api/",
		"api.track.inquiryNumber=1Z12345E0205271688"
})
class TrackOfflineTest {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AppConfig appConfig;

	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	void sendRequestReturnsParsedShipment() {
		server.expect(request -> {
			assertEquals(HttpMethod.POST, request.getMethod());
			assertEquals("http://localhost/security/v1/oauth/token", request.getURI().toString());
		}).andRespond(withSuccess(
				"{\"token_type\":\"Bearer\",\"issued_at\":\"1\",\"client_id\":\"test-client\","
						+ "\"access_token\":\"test-token\",\"expires_in\":\"3600\",\"status\":\"approved\"}",
				MediaType.APPLICATION_JSON));
		server.expect(request -> {
			assertEquals(HttpMethod.GET, request.getMethod());
			assertEquals("Bearer test-token", request.getHeaders().getFirst("Authorization"));
			assertTrue(request.getURI().toString()
					.startsWith("http://localhost/api/track/v1/details/1Z12345E0205271688"));
		}).andRespond(withSuccess(
				"{\"trackResponse\":{\"shipment\":[{\"inquiryNumber\":\"1Z12345E0205271688\","
						+ "\"package\":[{\"activity\":[{}]}]}]}}",
				MediaType.APPLICATION_JSON));

		TrackApiResponse response = new Track(restTemplate, appConfig).sendRequest(
				"1Z12345E0205271688", "test-transaction-id", appConfig.getTransactionSrc(), "en_US", "false");

		assertEquals("1Z12345E0205271688",
				response.getTrackResponse().getShipment().get(0).getInquiryNumber());
		server.verify();
	}
}
