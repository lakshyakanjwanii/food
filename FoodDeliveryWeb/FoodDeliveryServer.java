import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class FoodDeliveryServer {

    static final String DB_URL  = "jdbc:mysql://localhost:3306/food_delivery";
    static final String DB_USER = System.getenv().getOrDefault("DB_USER", "root");
    static final String DB_PASS = System.getenv().getOrDefault("DB_PASS", "Devka@123");
    static final int    PORT    = 8080;

    // ─── DB ───────────────────────────────────────────────────────────────────
    static Connection getConn() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    // ─── MAIN ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/",           new RootHandler());
        server.createContext("/customers",  new CustomerHandler());
        server.createContext("/restaurants",new RestaurantHandler());
        server.createContext("/orders",     new OrderHandler());
        server.createContext("/partners",   new PartnerHandler());
        server.createContext("/static",     new StaticHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("✅ SwiftBite running at http://localhost:" + PORT);
    }

    // ─── SECURITY HEADERS ─────────────────────────────────────────────────────
    static void secureHeaders(HttpExchange ex) {
        Headers h = ex.getResponseHeaders();
        h.set("X-Content-Type-Options", "nosniff");
        h.set("X-Frame-Options", "DENY");
        h.set("X-XSS-Protection", "1; mode=block");
        h.set("Referrer-Policy", "no-referrer");
        h.set("Content-Security-Policy",
              "default-src 'self' https://fonts.googleapis.com https://fonts.gstatic.com 'unsafe-inline'");
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────────
    static Map<String,String> parseForm(String body) {
        Map<String,String> map = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) return map;
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], "UTF-8");
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "";
                map.put(key, val);
            } catch (Exception ignored) {}
        }
        return map;
    }

    static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    static void send(HttpExchange ex, String html) throws IOException {
        secureHeaders(ex); // ✅ ADDED
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.getResponseBody().close();
    }

    static void redirect(HttpExchange ex, String path) throws IOException {
        ex.getResponseHeaders().set("Location", path);
        ex.sendResponseHeaders(302, -1);
        ex.getResponseBody().close();
    }

    static String htmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;");
    }

    static Map<String,String> parseQuery(String query) {
        Map<String,String> map = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=",2);
            try { map.put(URLDecoder.decode(kv[0],"UTF-8"), kv.length>1 ? URLDecoder.decode(kv[1],"UTF-8"):""); }
            catch (Exception ignored) {}
        }
        return map;
    }

    // ─── ROOT ─────────────────────────────────────────────────────────────────
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            send(ex, "<h1>Server Running Securely ✅</h1>");
        }
    }

    static class CustomerHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            send(ex, "<h1>Customers Endpoint</h1>");
        }
    }

    static class RestaurantHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            send(ex, "<h1>Restaurants Endpoint</h1>");
        }
    }

    static class OrderHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            send(ex, "<h1>Orders Endpoint</h1>");
        }
    }

    static class PartnerHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            send(ex, "<h1>Partners Endpoint</h1>");
        }
    }

    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            ex.sendResponseHeaders(404, -1);
        }
    }
}