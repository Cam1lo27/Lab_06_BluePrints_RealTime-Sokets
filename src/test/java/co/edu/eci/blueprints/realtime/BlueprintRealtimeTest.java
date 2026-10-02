package co.edu.eci.blueprints.realtime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba de punta a punta del tiempo real con STOMP:
 * dos clientes en el mismo plano reciben el punto, un cliente en otro plano no,
 * el punto queda guardado (REST) y sin JWT no se puede conectar.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BlueprintRealtimeTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    String token;
    String author;
    String name;

    @BeforeEach
    void setUp() {
        token = login();
        author = "rt-" + UUID.randomUUID().toString().substring(0, 8);
        name = "plano-1";
        createBlueprint(author, name);
        createBlueprint(author, "otro-plano");
    }

    @AfterEach
    void tearDown() {
        // no dejar planos de prueba en la base
        for (String bp : List.of(name, "otro-plano")) {
            rest.exchange("/api/v1/blueprints/" + author + "/" + bp, HttpMethod.DELETE, new HttpEntity<>(auth()), Map.class);
        }
    }

    @Test
    void puntoPublicadoLlegaATodosLosClientesDelMismoPlanoYSeGuarda() throws Exception {
        StompSession clienteA = connect(token);
        StompSession clienteB = connect(token);
        StompSession clienteOtroPlano = connect(token);

        BlockingQueue<BlueprintUpdate> recibidosA = subscribe(clienteA, author, name);
        BlockingQueue<BlueprintUpdate> recibidosB = subscribe(clienteB, author, name);
        BlockingQueue<BlueprintUpdate> recibidosOtro = subscribe(clienteOtroPlano, author, "otro-plano");
        Thread.sleep(300); // dar tiempo a que las suscripciones queden registradas

        clienteA.send("/app/draw", Map.of("author", author, "name", name, "point", Map.of("x", 120, "y", 80)));

        BlueprintUpdate updA = recibidosA.poll(5, TimeUnit.SECONDS);
        BlueprintUpdate updB = recibidosB.poll(5, TimeUnit.SECONDS);
        assertThat(updA).as("el emisor también recibe la actualización").isNotNull();
        assertThat(updB).as("el otro cliente del mismo plano recibe la actualización").isNotNull();
        assertThat(updB.points()).extracting("x", "y").containsExactly(org.assertj.core.groups.Tuple.tuple(120, 80));

        assertThat(recibidosOtro.poll(1, TimeUnit.SECONDS))
                .as("un cliente suscrito a otro plano NO recibe nada (aislamiento por tópico)")
                .isNull();

        ResponseEntity<Map> guardado = rest.exchange(
                "/api/v1/blueprints/" + author + "/" + name, HttpMethod.GET, new HttpEntity<>(auth()), Map.class);
        Map<?, ?> data = (Map<?, ?>) guardado.getBody().get("data");
        assertThat((List<?>) data.get("points")).as("el punto quedó persistido").hasSize(1);

        clienteA.disconnect();
        clienteB.disconnect();
        clienteOtroPlano.disconnect();
    }

    @Test
    void sinTokenNoSePuedeConectar() {
        assertThatThrownBy(() -> connect(null)).isInstanceOf(Exception.class);
    }

    // ---------- helpers ----------

    private StompSession connect(String jwt) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connectHeaders = new StompHeaders();
        if (jwt != null) connectHeaders.add("Authorization", "Bearer " + jwt);
        return client.connectAsync("ws://localhost:" + port + "/ws-blueprints",
                        new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<BlueprintUpdate> subscribe(StompSession session, String author, String name) {
        BlockingQueue<BlueprintUpdate> queue = new LinkedBlockingQueue<>();
        session.subscribe(BlueprintRealtimeController.topicOf(author, name), new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return BlueprintUpdate.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) { queue.add((BlueprintUpdate) payload); }
        });
        return queue;
    }

    private String login() {
        ResponseEntity<Map> res = rest.postForEntity("/auth/login",
                Map.of("username", "student", "password", "student123"), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) res.getBody().get("access_token");
    }

    private void createBlueprint(String author, String name) {
        ResponseEntity<Map> res = rest.exchange("/api/v1/blueprints", HttpMethod.POST,
                new HttpEntity<>(Map.of("author", author, "name", name, "points", List.of()), auth()), Map.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private HttpHeaders auth() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }
}
