package vn.edu.phenikaa.unicateen.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * TC-AUTH-03 o muc hanh vi (BR43): phien bi huy sau mot khoang khong thao tac.
 * Chay may chu that tren cong ngau nhien; thoi gian het phien rut xuong 1 phut de test khong phai cho 30 phut.
 * Tomcat kiem tra han phien khi co yeu cau toi, nen sau 65 giay khong thao tac yeu cau ke tiep phai bi tu choi.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.servlet.session.timeout=1m")
class SessionTimeoutIntegrationTest extends IntegrationTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> me(String cookie) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/me"))
                .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void TC_AUTH_03_phienHetHanSauThoiGianKhongThaoTac() throws Exception {
        HttpResponse<String> login = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"banhang1@phenikaa-uni.edu.vn\",\"password\":\"Demo@2026\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, login.statusCode(), login.body());
        String cookie = login.headers().firstValue("Set-Cookie").map(c -> c.split(";")[0]).orElse(null);
        assertNotNull(cookie, "may chu phai cap cookie phien");

        assertEquals(200, me(cookie).statusCode(), "phien con hieu luc ngay sau dang nhap");
        Thread.sleep(65_000);
        assertEquals(401, me(cookie).statusCode(), "sau 65 giay khong thao tac phien phai het han");
    }
}
