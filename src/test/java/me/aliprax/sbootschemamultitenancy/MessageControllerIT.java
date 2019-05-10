package me.aliprax.sbootschemamultitenancy;


import me.aliprax.sbootschemamultitenancy.database.entities.Message;
import me.aliprax.sbootschemamultitenancy.database.entities.Tenant;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class MessageControllerIT {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    public void testMessageCreation(){
        Tenant tenant = new Tenant();
        tenant.setSchemaName("MESSAGE_TEST_POST");
        tenant.setTenantName("testMessageCreation");
        ResponseEntity<Tenant> response = restTemplate.postForEntity("/tenants",tenant,Tenant.class);
        assertThat(response.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
        assertThat(response.getBody()).hasFieldOrProperty("uuid");

        String tenantUuid = response.getBody().getUuid();
        Message message = new Message();
        message.setMessage("This is a test message");
        ResponseEntity<Message> responsePost = restTemplate.exchange(RequestEntity
                .post(URI.create("/messages"))
                .header("tenant-uuid",tenantUuid)
                .body(message), Message.class);
        assertThat(responsePost.getStatusCode()).isEqualByComparingTo(HttpStatus.CREATED);
        assertThat(responsePost.getBody())
                .hasFieldOrProperty("uuid")
                .hasFieldOrPropertyWithValue("message",message.getMessage());
        String messageUuid = responsePost.getBody().getUuid();

        // Construct a header with the tenant-uuid to delete the message
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("tenant-uuid", tenantUuid);
        HttpEntity<?> request = new HttpEntity<Object>(headers);
        restTemplate.exchange("/messages/1?uuid="+messageUuid, HttpMethod.DELETE, request, String.class);

        // Delete the tenant we created for this test
        restTemplate.delete("/tenants/1?uuid={uuid}",tenantUuid);
    }

    @Test
    public void testMessageDeletion(){
        Tenant tenant = new Tenant();
        tenant.setSchemaName("MESSAGE_TEST_DELETE");
        tenant.setTenantName("testMessageDeletion");
        ResponseEntity<Tenant> response = restTemplate.postForEntity("/tenants",tenant,Tenant.class);

        String tenantUuid = response.getBody().getUuid();
        Message message = new Message();
        message.setMessage("This is a test message");
        ResponseEntity<Message> responsePost = restTemplate.exchange(RequestEntity
                .post(URI.create("/messages"))
                .header("tenant-uuid",tenantUuid)
                .body(message), Message.class);
        String messageUuid = responsePost.getBody().getUuid();

        // Construct a header with the tenant-uuid to delete the message
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("tenant-uuid", tenantUuid);
        HttpEntity<?> request = new HttpEntity<Object>(headers);
        restTemplate.exchange("/messages/1?uuid="+messageUuid, HttpMethod.DELETE, request, String.class);

        // Get the list of messages available and check to ensure it is empty
        ResponseEntity<RestPageImpl> getResponse  = restTemplate.exchange("/messages",
                HttpMethod.GET, request, RestPageImpl.class);

        assertThat(getResponse.getBody().getTotalElements()).isEqualByComparingTo(0L);

        // Delete the tenant we created for this test
        restTemplate.delete("/tenants/1?uuid={uuid}",tenantUuid);

        // Get the list of tenants available and check to ensure it is empty
        getResponse  = restTemplate.exchange("/tenants",
                HttpMethod.GET, null, RestPageImpl.class);
        assertThat(getResponse.getBody().getTotalElements()).isEqualByComparingTo(0L);
    }

}