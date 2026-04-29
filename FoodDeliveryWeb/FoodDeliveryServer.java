import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;

public class FoodDeliveryServer {

    static final String DB_URL  = "jdbc:mysql://localhost:3306/food_delivery";
    static final String DB_USER = "root";
    static final String DB_PASS = "Devka@123";
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
        System.out.println("   Open your browser and go to: http://localhost:" + PORT);
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

    // ─── CSS / LAYOUT ─────────────────────────────────────────────────────────
    static String css() {
        return """
        <style>
        @import url('https://fonts.googleapis.com/css2?family=Playfair+Display:wght@700;900&family=DM+Sans:wght@300;400;500;600&display=swap');
        *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
        :root{
          --bg:#0a0a0a;--surface:#141414;--surface2:#1e1e1e;--border:#272727;
          --accent:#ff6b2b;--accent2:#ff9a5c;--text:#f0ece4;--muted:#777;
          --success:#3ecf6e;--danger:#e05555;--warning:#f5c842;
          --font-display:'Playfair Display',serif;--font-body:'DM Sans',sans-serif;
          --radius:10px;
        }
        body{background:var(--bg);color:var(--text);font-family:var(--font-body);display:flex;min-height:100vh}
        a{color:inherit;text-decoration:none}
        /* Sidebar */
        .sidebar{width:230px;min-height:100vh;background:var(--surface);border-right:1px solid var(--border);
          display:flex;flex-direction:column;position:fixed;top:0;left:0;z-index:100}
        .logo{padding:26px 22px 18px;font-family:var(--font-display);font-size:21px;font-weight:900;
          color:var(--accent);border-bottom:1px solid var(--border);letter-spacing:-0.5px}
        .logo span{color:var(--text)}
        .nav{padding:14px 10px;flex:1}
        .nav-label{font-size:10px;font-weight:600;color:var(--muted);text-transform:uppercase;
          letter-spacing:.12em;padding:8px 12px 4px}
        .nav-item{display:flex;align-items:center;gap:10px;padding:9px 13px;border-radius:var(--radius);
          color:var(--muted);font-size:13px;font-weight:500;transition:all .15s;margin-bottom:2px}
        .nav-item:hover{background:var(--surface2);color:var(--text)}
        .nav-item.active{background:rgba(255,107,43,.15);color:var(--accent)}
        /* Main */
        .main{margin-left:230px;flex:1;padding:30px 34px;min-height:100vh}
        .page-header{margin-bottom:24px}
        .page-header h1{font-family:var(--font-display);font-size:28px;font-weight:900;letter-spacing:-.5px}
        .page-header p{color:var(--muted);font-size:13px;margin-top:4px}
        /* Stats */
        .stats{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:24px}
        .stat{background:var(--surface);border:1px solid var(--border);border-radius:var(--radius);padding:18px 22px}
        .stat-icon{font-size:26px;margin-bottom:8px}
        .stat-val{font-family:var(--font-display);font-size:30px;font-weight:900;color:var(--accent)}
        .stat-lbl{font-size:12px;color:var(--muted);margin-top:2px}
        /* Card */
        .card{background:var(--surface);border:1px solid var(--border);border-radius:var(--radius);padding:22px;margin-bottom:20px}
        .card-title{font-size:12px;font-weight:600;color:var(--muted);text-transform:uppercase;letter-spacing:.08em;margin-bottom:14px}
        /* Table */
        .tbl-wrap{overflow-x:auto}
        table{width:100%;border-collapse:collapse;font-size:13px}
        thead th{text-align:left;padding:9px 13px;font-size:10px;font-weight:600;color:var(--muted);
          text-transform:uppercase;letter-spacing:.08em;border-bottom:1px solid var(--border)}
        tbody tr{border-bottom:1px solid var(--border);transition:background .1s}
        tbody tr:hover{background:var(--surface2)}
        tbody td{padding:11px 13px}
        /* Form */
        .form-row{display:grid;grid-template-columns:repeat(auto-fit,minmax(180px,1fr));gap:12px;margin-bottom:14px}
        .fg{display:flex;flex-direction:column;gap:5px}
        label{font-size:11px;font-weight:600;color:var(--muted);text-transform:uppercase;letter-spacing:.06em}
        input,select{background:var(--surface2);border:1.5px solid var(--border);border-radius:8px;
          padding:9px 13px;color:var(--text);font-size:13px;font-family:var(--font-body);
          outline:none;transition:border-color .15s;width:100%}
        input:focus,select:focus{border-color:var(--accent)}
        input::placeholder{color:var(--muted)}
        select option{background:var(--surface2)}
        /* Buttons */
        .btn{display:inline-flex;align-items:center;gap:5px;padding:8px 16px;border-radius:8px;
          border:none;font-size:12px;font-weight:600;cursor:pointer;font-family:var(--font-body);transition:all .15s}
        .btn-primary{background:var(--accent);color:#fff}
        .btn-primary:hover{background:var(--accent2)}
        .btn-danger{background:rgba(224,85,85,.12);color:var(--danger);border:1px solid rgba(224,85,85,.25)}
        .btn-danger:hover{background:var(--danger);color:#fff}
        .btn-edit{background:rgba(245,200,66,.1);color:var(--warning);border:1px solid rgba(245,200,66,.2)}
        .btn-edit:hover{background:var(--warning);color:#000}
        .btn-sm{padding:5px 10px;font-size:11px}
        .act{display:flex;gap:5px}
        .sec-hdr{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}
        /* Alert */
        .alert{padding:11px 15px;border-radius:var(--radius);font-size:13px;margin-bottom:18px}
        .alert-s{background:rgba(62,207,110,.1);color:var(--success);border:1px solid rgba(62,207,110,.2)}
        .alert-e{background:rgba(224,85,85,.1);color:var(--danger);border:1px solid rgba(224,85,85,.2)}
        /* Badge */
        .badge{display:inline-block;padding:2px 9px;border-radius:20px;font-size:10px;font-weight:600;text-transform:uppercase;letter-spacing:.05em}
        .bp{background:#2a2200;color:var(--warning)}
        .bd{background:#0a2a1a;color:var(--success)}
        .bc{background:#2a0a0a;color:var(--danger)}
        .bo{background:#0a1a2a;color:#5bc4f5}
        /* Modal */
        .modal-bg{display:none;position:fixed;inset:0;background:rgba(0,0,0,.75);z-index:200;
          align-items:center;justify-content:center}
        .modal-bg.open{display:flex}
        .modal{background:var(--surface);border:1px solid var(--border);border-radius:14px;
          padding:26px;width:100%;max-width:480px}
        .modal h3{font-family:var(--font-display);font-size:19px;font-weight:700;margin-bottom:18px}
        .modal-foot{display:flex;gap:8px;justify-content:flex-end;margin-top:18px}
        </style>
        <script>
        function openModal(id){document.getElementById(id).classList.add('open')}
        function closeModal(id){document.getElementById(id).classList.remove('open')}
        function fillEdit(formId,data){
          var f=document.getElementById(formId);
          Object.keys(data).forEach(function(k){if(f.elements[k])f.elements[k].value=data[k]});
          openModal(formId+'-modal');
        }
        window.onclick=function(e){
          document.querySelectorAll('.modal-bg.open').forEach(function(m){if(e.target===m)m.classList.remove('open')});
        }
        </script>
        """;
    }

    static String nav(String active) {
        String[] pages = {"/","customers","restaurants","orders","partners"};
        String[] icons = {"🏠","👤","🏪","📦","🚴"};
        String[] names = {"Dashboard","Customers","Restaurants","Orders","Delivery Partners"};
        StringBuilder sb = new StringBuilder();
        sb.append("<aside class='sidebar'>");
        sb.append("<div class='logo'>🍜 Swift<span>Bite</span></div>");
        sb.append("<nav class='nav'><div class='nav-label'>Menu</div>");
        for (int i = 0; i < pages.length; i++) {
            String href = i == 0 ? "/" : "/" + pages[i];
            String cls  = pages[i].equals(active) ? " active" : "";
            sb.append("<a href='").append(href).append("' class='nav-item").append(cls).append("'>")
              .append("<span>").append(icons[i]).append("</span> ").append(names[i]).append("</a>");
        }
        sb.append("</nav></aside>");
        return sb.toString();
    }

    static String wrap(String active, String title, String body) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'/>" +
               "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
               "<title>" + title + " | SwiftBite</title>" + css() + "</head><body>" +
               nav(active) + "<main class='main'>" + body + "</main></body></html>";
    }

    static String alert(Map<String,String> params) {
        String msg = params.getOrDefault("msg","");
        String type= params.getOrDefault("type","");
        if (msg.isEmpty()) return "";
        String cls = type.equals("s") ? "alert-s" : "alert-e";
        return "<div class='alert " + cls + "'>" + htmlEsc(msg) + "</div>";
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

    // ─── ROOT / DASHBOARD ─────────────────────────────────────────────────────
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            long customers=0,restaurants=0,orders=0,partners=0;
            try (Connection c = getConn()) {
                customers   = ((Number)c.createStatement().executeQuery("SELECT COUNT(*) FROM Customer").getObject(1)).longValue();
                restaurants = ((Number)c.createStatement().executeQuery("SELECT COUNT(*) FROM Restaurant").getObject(1)).longValue();
                orders      = ((Number)c.createStatement().executeQuery("SELECT COUNT(*) FROM `Order`").getObject(1)).longValue();
                partners    = ((Number)c.createStatement().executeQuery("SELECT COUNT(*) FROM Delivery_Partner").getObject(1)).longValue();
            } catch (Exception e) { e.printStackTrace(); }

            String body = """
            <div class='page-header'><h1>Dashboard</h1><p>Food Delivery Management System</p></div>
            <div class='stats'>
              <div class='stat'><div class='stat-icon'>👤</div><div class='stat-val'>%d</div><div class='stat-lbl'>Customers</div></div>
              <div class='stat'><div class='stat-icon'>🏪</div><div class='stat-val'>%d</div><div class='stat-lbl'>Restaurants</div></div>
              <div class='stat'><div class='stat-icon'>📦</div><div class='stat-val'>%d</div><div class='stat-lbl'>Orders</div></div>
              <div class='stat'><div class='stat-icon'>🚴</div><div class='stat-val'>%d</div><div class='stat-lbl'>Delivery Partners</div></div>
            </div>
            <div style='display:grid;grid-template-columns:1fr 1fr;gap:16px'>
              <div class='card'>
                <div class='card-title'>Quick Actions</div>
                <div style='display:flex;flex-direction:column;gap:10px'>
                  <a href='/customers' class='btn btn-primary' style='justify-content:center'>👤 Manage Customers</a>
                  <a href='/restaurants' class='btn btn-primary' style='justify-content:center'>🏪 Manage Restaurants</a>
                  <a href='/orders' class='btn btn-primary' style='justify-content:center'>📦 Manage Orders</a>
                  <a href='/partners' class='btn btn-primary' style='justify-content:center'>🚴 Manage Partners</a>
                </div>
              </div>
              <div class='card'>
                <div class='card-title'>About</div>
                <p style='color:var(--muted);font-size:13px;line-height:1.9'>
                  DBMS Mini Project — Food Delivery System<br/>
                  Built with <strong style='color:var(--accent)'>Java</strong> +
                  <strong style='color:var(--accent)'>JDBC</strong> +
                  <strong style='color:var(--accent)'>MySQL</strong><br/>
                  Full CRUD on all ER entities: Customer, Restaurant, Order, Delivery Partner.
                </p>
              </div>
            </div>
            """.formatted(customers, restaurants, orders, partners);

            send(ex, wrap("/", "Dashboard", body));
        }
    }

    // ─── CUSTOMERS ────────────────────────────────────────────────────────────
    static class CustomerHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            String query  = ex.getRequestURI().getQuery();
            Map<String,String> qp = parseQuery(query);

            if ("POST".equals(method)) {
                Map<String,String> p = parseForm(readBody(ex));
                String action = p.getOrDefault("action","");
                try (Connection c = getConn()) {
                    if ("add".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO User(name,phone,role) VALUES(?,?,'customer')",
                            Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1,p.get("name")); ps.setString(2,p.get("phone")); ps.executeUpdate();
                        ResultSet rs = ps.getGeneratedKeys();
                        if (rs.next()) {
                            PreparedStatement ps2 = c.prepareStatement("INSERT INTO Customer(user_id,address) VALUES(?,?)");
                            ps2.setInt(1,rs.getInt(1)); ps2.setString(2,p.get("address")); ps2.executeUpdate();
                        }
                        redirect(ex,"/customers?msg=Customer+added+successfully&type=s"); return;
                    } else if ("update".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "UPDATE User u JOIN Customer cu ON u.user_id=cu.user_id SET u.name=?,u.phone=?,cu.address=? WHERE cu.customer_id=?");
                        ps.setString(1,p.get("name")); ps.setString(2,p.get("phone"));
                        ps.setString(3,p.get("address")); ps.setInt(4,Integer.parseInt(p.get("id")));
                        ps.executeUpdate();
                        redirect(ex,"/customers?msg=Customer+updated&type=s"); return;
                    } else if ("delete".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "DELETE u FROM User u JOIN Customer cu ON u.user_id=cu.user_id WHERE cu.customer_id=?");
                        ps.setInt(1,Integer.parseInt(p.get("id"))); ps.executeUpdate();
                        redirect(ex,"/customers?msg=Customer+deleted&type=s"); return;
                    }
                } catch (Exception e) {
                    redirect(ex,"/customers?msg="+URLEncoder.encode(e.getMessage(),"UTF-8")+"&type=e"); return;
                }
            }

            // GET - render page
            StringBuilder rows = new StringBuilder();
            try (Connection c = getConn()) {
                ResultSet rs = c.createStatement().executeQuery(
                    "SELECT cu.customer_id,u.name,u.phone,cu.address FROM Customer cu JOIN User u ON cu.user_id=u.user_id");
                while (rs.next()) {
                    int id = rs.getInt(1); String name=rs.getString(2),phone=rs.getString(3),addr=rs.getString(4);
                    rows.append("<tr><td>").append(id).append("</td>")
                        .append("<td>").append(htmlEsc(name)).append("</td>")
                        .append("<td>").append(htmlEsc(phone)).append("</td>")
                        .append("<td>").append(htmlEsc(addr)).append("</td>")
                        .append("<td><div class='act'>")
                        .append("<button class='btn btn-edit btn-sm' onclick=\"fillEdit('edit-customer',{id:'").append(id)
                        .append("',name:'").append(htmlEsc(name)).append("',phone:'").append(htmlEsc(phone))
                        .append("',address:'").append(htmlEsc(addr)).append("'})\">✏️ Edit</button>")
                        .append("<form method='POST' action='/customers' style='display:inline' onsubmit=\"return confirm('Delete this customer?')\">")
                        .append("<input type='hidden' name='action' value='delete'/>")
                        .append("<input type='hidden' name='id' value='").append(id).append("'/>")
                        .append("<button class='btn btn-danger btn-sm'>🗑 Delete</button></form>")
                        .append("</div></td></tr>");
                }
            } catch (Exception e) { rows.append("<tr><td colspan='5'>Error: ").append(e.getMessage()).append("</td></tr>"); }

            String body = alert(qp) + """
            <div class='page-header'><h1>Customers</h1><p>Manage all registered customers</p></div>
            <div class='card'>
              <div class='sec-hdr'>
                <div class='card-title' style='margin:0'>All Customers</div>
                <button class='btn btn-primary' onclick="openModal('add-customer-modal')">+ Add Customer</button>
              </div>
              <div class='tbl-wrap'>
                <table><thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Address</th><th>Actions</th></tr></thead>
                <tbody>""" + rows + """
                </tbody></table>
              </div>
            </div>
            <!-- Add Modal -->
            <div id='add-customer-modal' class='modal-bg'>
              <div class='modal'>
                <h3>Add Customer</h3>
                <form method='POST' action='/customers'>
                  <input type='hidden' name='action' value='add'/>
                  <div class='form-row'>
                    <div class='fg'><label>Name</label><input name='name' placeholder='Full name' required/></div>
                    <div class='fg'><label>Phone</label><input name='phone' placeholder='Phone number' required/></div>
                  </div>
                  <div class='fg' style='margin-bottom:14px'><label>Address</label><input name='address' placeholder='Delivery address' required/></div>
                  <div class='modal-foot'>
                    <button type='button' class='btn' onclick="closeModal('add-customer-modal')" style='background:var(--surface2)'>Cancel</button>
                    <button type='submit' class='btn btn-primary'>Save Customer</button>
                  </div>
                </form>
              </div>
            </div>
            <!-- Edit Modal -->
            <div id='edit-customer-modal' class='modal-bg'>
              <div class='modal'>
                <h3>Edit Customer</h3>
                <form method='POST' action='/customers' id='edit-customer'>
                  <input type='hidden' name='action' value='update'/>
                  <input type='hidden' name='id'/>
                  <div class='form-row'>
                    <div class='fg'><label>Name</label><input name='name' required/></div>
                    <div class='fg'><label>Phone</label><input name='phone' required/></div>
                  </div>
                  <div class='fg' style='margin-bottom:14px'><label>Address</label><input name='address' required/></div>
                  <div class='modal-foot'>
                    <button type='button' class='btn' onclick="closeModal('edit-customer-modal')" style='background:var(--surface2)'>Cancel</button>
                    <button type='submit' class='btn btn-primary'>Update Customer</button>
                  </div>
                </form>
              </div>
            </div>
            """;
            send(ex, wrap("customers","Customers",body));
        }
    }

    // ─── RESTAURANTS ──────────────────────────────────────────────────────────
    static class RestaurantHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            Map<String,String> qp = parseQuery(ex.getRequestURI().getQuery());

            if ("POST".equals(method)) {
                Map<String,String> p = parseForm(readBody(ex));
                String action = p.getOrDefault("action","");
                try (Connection c = getConn()) {
                    if ("add".equals(action)) {
                        PreparedStatement ps = c.prepareStatement("INSERT INTO Restaurant(name,location,rating) VALUES(?,?,?)");
                        ps.setString(1,p.get("name")); ps.setString(2,p.get("location")); ps.setDouble(3,Double.parseDouble(p.get("rating")));
                        ps.executeUpdate();
                        redirect(ex,"/restaurants?msg=Restaurant+added&type=s"); return;
                    } else if ("update".equals(action)) {
                        PreparedStatement ps = c.prepareStatement("UPDATE Restaurant SET name=?,location=?,rating=? WHERE restaurant_id=?");
                        ps.setString(1,p.get("name")); ps.setString(2,p.get("location")); ps.setDouble(3,Double.parseDouble(p.get("rating"))); ps.setInt(4,Integer.parseInt(p.get("id")));
                        ps.executeUpdate();
                        redirect(ex,"/restaurants?msg=Restaurant+updated&type=s"); return;
                    } else if ("delete".equals(action)) {
                        PreparedStatement ps = c.prepareStatement("DELETE FROM Restaurant WHERE restaurant_id=?");
                        ps.setInt(1,Integer.parseInt(p.get("id"))); ps.executeUpdate();
                        redirect(ex,"/restaurants?msg=Restaurant+deleted&type=s"); return;
                    }
                } catch (Exception e) {
                    redirect(ex,"/restaurants?msg="+URLEncoder.encode(e.getMessage(),"UTF-8")+"&type=e"); return;
                }
            }

            StringBuilder rows = new StringBuilder();
            try (Connection c = getConn()) {
                ResultSet rs = c.createStatement().executeQuery("SELECT * FROM Restaurant ORDER BY rating DESC");
                while (rs.next()) {
                    int id=rs.getInt(1); String name=rs.getString(2),loc=rs.getString(3); double rat=rs.getDouble(4);
                    rows.append("<tr><td>").append(id).append("</td>")
                        .append("<td>").append(htmlEsc(name)).append("</td>")
                        .append("<td>").append(htmlEsc(loc)).append("</td>")
                        .append("<td>⭐ ").append(rat).append("</td>")
                        .append("<td><div class='act'>")
                        .append("<button class='btn btn-edit btn-sm' onclick=\"fillEdit('edit-restaurant',{id:'").append(id)
                        .append("',name:'").append(htmlEsc(name)).append("',location:'").append(htmlEsc(loc))
                        .append("',rating:'").append(rat).append("'})\">✏️ Edit</button>")
                        .append("<form method='POST' action='/restaurants' style='display:inline' onsubmit=\"return confirm('Delete?')\">")
                        .append("<input type='hidden' name='action' value='delete'/>")
                        .append("<input type='hidden' name='id' value='").append(id).append("'/>")
                        .append("<button class='btn btn-danger btn-sm'>🗑 Delete</button></form>")
                        .append("</div></td></tr>");
                }
            } catch (Exception e) { rows.append("<tr><td colspan='5'>Error: ").append(e.getMessage()).append("</td></tr>"); }

            String body = alert(qp) + """
            <div class='page-header'><h1>Restaurants</h1><p>Manage all restaurants</p></div>
            <div class='card'>
              <div class='sec-hdr'>
                <div class='card-title' style='margin:0'>All Restaurants</div>
                <button class='btn btn-primary' onclick="openModal('add-restaurant-modal')">+ Add Restaurant</button>
              </div>
              <div class='tbl-wrap'>
                <table><thead><tr><th>ID</th><th>Name</th><th>Location</th><th>Rating</th><th>Actions</th></tr></thead>
                <tbody>""" + rows + """
                </tbody></table>
              </div>
            </div>
            <div id='add-restaurant-modal' class='modal-bg'><div class='modal'>
              <h3>Add Restaurant</h3>
              <form method='POST' action='/restaurants'>
                <input type='hidden' name='action' value='add'/>
                <div class='form-row'>
                  <div class='fg'><label>Name</label><input name='name' placeholder='Restaurant name' required/></div>
                  <div class='fg'><label>Location</label><input name='location' placeholder='Location' required/></div>
                  <div class='fg'><label>Rating (0-5)</label><input name='rating' type='number' step='0.1' min='0' max='5' placeholder='4.5' required/></div>
                </div>
                <div class='modal-foot'>
                  <button type='button' class='btn' onclick="closeModal('add-restaurant-modal')" style='background:var(--surface2)'>Cancel</button>
                  <button type='submit' class='btn btn-primary'>Save</button>
                </div>
              </form>
            </div></div>
            <div id='edit-restaurant-modal' class='modal-bg'><div class='modal'>
              <h3>Edit Restaurant</h3>
              <form method='POST' action='/restaurants' id='edit-restaurant'>
                <input type='hidden' name='action' value='update'/>
                <input type='hidden' name='id'/>
                <div class='form-row'>
                  <div class='fg'><label>Name</label><input name='name' required/></div>
                  <div class='fg'><label>Location</label><input name='location' required/></div>
                  <div class='fg'><label>Rating</label><input name='rating' type='number' step='0.1' min='0' max='5' required/></div>
                </div>
                <div class='modal-foot'>
                  <button type='button' class='btn' onclick="closeModal('edit-restaurant-modal')" style='background:var(--surface2)'>Cancel</button>
                  <button type='submit' class='btn btn-primary'>Update</button>
                </div>
              </form>
            </div></div>
            """;
            send(ex, wrap("restaurants","Restaurants",body));
        }
    }

    // ─── ORDERS ───────────────────────────────────────────────────────────────
    static class OrderHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            Map<String,String> qp = parseQuery(ex.getRequestURI().getQuery());

            if ("POST".equals(method)) {
                Map<String,String> p = parseForm(readBody(ex));
                String action = p.getOrDefault("action","");
                try (Connection c = getConn()) {
                    if ("add".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO `Order`(customer_id,restaurant_id,address,status) VALUES(?,?,?,'pending')",
                            Statement.RETURN_GENERATED_KEYS);
                        ps.setInt(1,Integer.parseInt(p.get("customer_id")));
                        ps.setInt(2,Integer.parseInt(p.get("restaurant_id")));
                        ps.setString(3,p.get("address")); ps.executeUpdate();
                        ResultSet rs = ps.getGeneratedKeys();
                        if (rs.next()) {
                            PreparedStatement ps2 = c.prepareStatement("INSERT INTO Order_Item(order_id,qty,price) VALUES(?,?,?)");
                            ps2.setInt(1,rs.getInt(1)); ps2.setInt(2,Integer.parseInt(p.get("qty"))); ps2.setDouble(3,Double.parseDouble(p.get("price")));
                            ps2.executeUpdate();
                        }
                        redirect(ex,"/orders?msg=Order+placed+successfully&type=s"); return;
                    } else if ("update".equals(action)) {
                        PreparedStatement ps = c.prepareStatement("UPDATE `Order` SET address=?,status=? WHERE order_id=?");
                        ps.setString(1,p.get("address")); ps.setString(2,p.get("status")); ps.setInt(3,Integer.parseInt(p.get("id")));
                        ps.executeUpdate();
                        redirect(ex,"/orders?msg=Order+updated&type=s"); return;
                    } else if ("delete".equals(action)) {
                        PreparedStatement ps = c.prepareStatement("DELETE FROM `Order` WHERE order_id=?");
                        ps.setInt(1,Integer.parseInt(p.get("id"))); ps.executeUpdate();
                        redirect(ex,"/orders?msg=Order+deleted&type=s"); return;
                    }
                } catch (Exception e) {
                    redirect(ex,"/orders?msg="+URLEncoder.encode(e.getMessage(),"UTF-8")+"&type=e"); return;
                }
            }

            StringBuilder rows = new StringBuilder();
            StringBuilder custOpts = new StringBuilder();
            StringBuilder restOpts = new StringBuilder();
            try (Connection c = getConn()) {
                ResultSet rs = c.createStatement().executeQuery(
                    "SELECT o.order_id,u.name,r.name,o.address,o.status FROM `Order` o "+
                    "JOIN Customer cu ON o.customer_id=cu.customer_id "+
                    "JOIN User u ON cu.user_id=u.user_id "+
                    "JOIN Restaurant r ON o.restaurant_id=r.restaurant_id ORDER BY o.order_id DESC");
                while (rs.next()) {
                    int id=rs.getInt(1); String cust=rs.getString(2),rest=rs.getString(3),addr=rs.getString(4),status=rs.getString(5);
                    String badgeCls = status.equals("delivered")?"bd":status.equals("cancelled")?"bc":status.equals("out_for_delivery")?"bo":"bp";
                    rows.append("<tr><td>").append(id).append("</td>")
                        .append("<td>").append(htmlEsc(cust)).append("</td>")
                        .append("<td>").append(htmlEsc(rest)).append("</td>")
                        .append("<td>").append(htmlEsc(addr)).append("</td>")
                        .append("<td><span class='badge ").append(badgeCls).append("'>").append(status).append("</span></td>")
                        .append("<td><div class='act'>")
                        .append("<button class='btn btn-edit btn-sm' onclick=\"fillEdit('edit-order',{id:'").append(id)
                        .append("',address:'").append(htmlEsc(addr)).append("',status:'").append(status).append("'})\">✏️ Edit</button>")
                        .append("<form method='POST' action='/orders' style='display:inline' onsubmit=\"return confirm('Delete order?')\">")
                        .append("<input type='hidden' name='action' value='delete'/>")
                        .append("<input type='hidden' name='id' value='").append(id).append("'/>")
                        .append("<button class='btn btn-danger btn-sm'>🗑 Delete</button></form>")
                        .append("</div></td></tr>");
                }
                ResultSet cr = c.createStatement().executeQuery("SELECT cu.customer_id,u.name FROM Customer cu JOIN User u ON cu.user_id=u.user_id");
                while (cr.next()) custOpts.append("<option value='").append(cr.getInt(1)).append("'>").append(htmlEsc(cr.getString(2))).append("</option>");
                ResultSet rr = c.createStatement().executeQuery("SELECT restaurant_id,name FROM Restaurant");
                while (rr.next()) restOpts.append("<option value='").append(rr.getInt(1)).append("'>").append(htmlEsc(rr.getString(2))).append("</option>");
            } catch (Exception e) { rows.append("<tr><td colspan='6'>Error: ").append(e.getMessage()).append("</td></tr>"); }

            String body = alert(qp) + """
            <div class='page-header'><h1>Orders</h1><p>Manage all orders</p></div>
            <div class='card'>
              <div class='sec-hdr'>
                <div class='card-title' style='margin:0'>All Orders</div>
                <button class='btn btn-primary' onclick="openModal('add-order-modal')">+ Place Order</button>
              </div>
              <div class='tbl-wrap'>
                <table><thead><tr><th>ID</th><th>Customer</th><th>Restaurant</th><th>Address</th><th>Status</th><th>Actions</th></tr></thead>
                <tbody>""" + rows + """
                </tbody></table>
              </div>
            </div>
            <div id='add-order-modal' class='modal-bg'><div class='modal'>
              <h3>Place New Order</h3>
              <form method='POST' action='/orders'>
                <input type='hidden' name='action' value='add'/>
                <div class='form-row'>
                  <div class='fg'><label>Customer</label><select name='customer_id' required><option value=''>Select...</option>""" + custOpts + """
                  </select></div>
                  <div class='fg'><label>Restaurant</label><select name='restaurant_id' required><option value=''>Select...</option>""" + restOpts + """
                  </select></div>
                </div>
                <div class='form-row'>
                  <div class='fg'><label>Delivery Address</label><input name='address' placeholder='Address' required/></div>
                  <div class='fg'><label>Qty</label><input name='qty' type='number' min='1' placeholder='1' required/></div>
                  <div class='fg'><label>Price (₹)</label><input name='price' type='number' step='0.01' placeholder='199.00' required/></div>
                </div>
                <div class='modal-foot'>
                  <button type='button' class='btn' onclick="closeModal('add-order-modal')" style='background:var(--surface2)'>Cancel</button>
                  <button type='submit' class='btn btn-primary'>Place Order</button>
                </div>
              </form>
            </div></div>
            <div id='edit-order-modal' class='modal-bg'><div class='modal'>
              <h3>Update Order</h3>
              <form method='POST' action='/orders' id='edit-order'>
                <input type='hidden' name='action' value='update'/>
                <input type='hidden' name='id'/>
                <div class='form-row'>
                  <div class='fg'><label>Address</label><input name='address' required/></div>
                  <div class='fg'><label>Status</label>
                    <select name='status'>
                      <option value='pending'>Pending</option>
                      <option value='confirmed'>Confirmed</option>
                      <option value='preparing'>Preparing</option>
                      <option value='out_for_delivery'>Out for Delivery</option>
                      <option value='delivered'>Delivered</option>
                      <option value='cancelled'>Cancelled</option>
                    </select>
                  </div>
                </div>
                <div class='modal-foot'>
                  <button type='button' class='btn' onclick="closeModal('edit-order-modal')" style='background:var(--surface2)'>Cancel</button>
                  <button type='submit' class='btn btn-primary'>Update</button>
                </div>
              </form>
            </div></div>
            """;
            send(ex, wrap("orders","Orders",body));
        }
    }

    // ─── PARTNERS ─────────────────────────────────────────────────────────────
    static class PartnerHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            String method = ex.getRequestMethod();
            Map<String,String> qp = parseQuery(ex.getRequestURI().getQuery());

            if ("POST".equals(method)) {
                Map<String,String> p = parseForm(readBody(ex));
                String action = p.getOrDefault("action","");
                try (Connection c = getConn()) {
                    if ("add".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO User(name,phone,role) VALUES(?,?,'delivery_partner')",
                            Statement.RETURN_GENERATED_KEYS);
                        ps.setString(1,p.get("name")); ps.setString(2,p.get("phone")); ps.executeUpdate();
                        ResultSet rs = ps.getGeneratedKeys();
                        if (rs.next()) {
                            PreparedStatement ps2 = c.prepareStatement("INSERT INTO Delivery_Partner(user_id,vehicle_type) VALUES(?,?)");
                            ps2.setInt(1,rs.getInt(1)); ps2.setString(2,p.get("vehicle_type")); ps2.executeUpdate();
                        }
                        redirect(ex,"/partners?msg=Partner+added&type=s"); return;
                    } else if ("update".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "UPDATE User u JOIN Delivery_Partner dp ON u.user_id=dp.user_id SET u.name=?,u.phone=?,dp.vehicle_type=? WHERE dp.partner_id=?");
                        ps.setString(1,p.get("name")); ps.setString(2,p.get("phone"));
                        ps.setString(3,p.get("vehicle_type")); ps.setInt(4,Integer.parseInt(p.get("id")));
                        ps.executeUpdate();
                        redirect(ex,"/partners?msg=Partner+updated&type=s"); return;
                    } else if ("delete".equals(action)) {
                        PreparedStatement ps = c.prepareStatement(
                            "DELETE u FROM User u JOIN Delivery_Partner dp ON u.user_id=dp.user_id WHERE dp.partner_id=?");
                        ps.setInt(1,Integer.parseInt(p.get("id"))); ps.executeUpdate();
                        redirect(ex,"/partners?msg=Partner+deleted&type=s"); return;
                    }
                } catch (Exception e) {
                    redirect(ex,"/partners?msg="+URLEncoder.encode(e.getMessage(),"UTF-8")+"&type=e"); return;
                }
            }

            StringBuilder rows = new StringBuilder();
            try (Connection c = getConn()) {
                ResultSet rs = c.createStatement().executeQuery(
                    "SELECT dp.partner_id,u.name,u.phone,dp.vehicle_type FROM Delivery_Partner dp JOIN User u ON dp.user_id=u.user_id");
                while (rs.next()) {
                    int id=rs.getInt(1); String name=rs.getString(2),phone=rs.getString(3),veh=rs.getString(4);
                    rows.append("<tr><td>").append(id).append("</td>")
                        .append("<td>").append(htmlEsc(name)).append("</td>")
                        .append("<td>").append(htmlEsc(phone)).append("</td>")
                        .append("<td>").append(htmlEsc(veh)).append("</td>")
                        .append("<td><div class='act'>")
                        .append("<button class='btn btn-edit btn-sm' onclick=\"fillEdit('edit-partner',{id:'").append(id)
                        .append("',name:'").append(htmlEsc(name)).append("',phone:'").append(htmlEsc(phone))
                        .append("',vehicle_type:'").append(htmlEsc(veh)).append("'})\">✏️ Edit</button>")
                        .append("<form method='POST' action='/partners' style='display:inline' onsubmit=\"return confirm('Delete?')\">")
                        .append("<input type='hidden' name='action' value='delete'/>")
                        .append("<input type='hidden' name='id' value='").append(id).append("'/>")
                        .append("<button class='btn btn-danger btn-sm'>🗑 Delete</button></form>")
                        .append("</div></td></tr>");
                }
            } catch (Exception e) { rows.append("<tr><td colspan='5'>Error: ").append(e.getMessage()).append("</td></tr>"); }

            String body = alert(qp) + """
            <div class='page-header'><h1>Delivery Partners</h1><p>Manage all delivery partners</p></div>
            <div class='card'>
              <div class='sec-hdr'>
                <div class='card-title' style='margin:0'>All Partners</div>
                <button class='btn btn-primary' onclick="openModal('add-partner-modal')">+ Add Partner</button>
              </div>
              <div class='tbl-wrap'>
                <table><thead><tr><th>ID</th><th>Name</th><th>Phone</th><th>Vehicle</th><th>Actions</th></tr></thead>
                <tbody>""" + rows + """
                </tbody></table>
              </div>
            </div>
            <div id='add-partner-modal' class='modal-bg'><div class='modal'>
              <h3>Add Delivery Partner</h3>
              <form method='POST' action='/partners'>
                <input type='hidden' name='action' value='add'/>
                <div class='form-row'>
                  <div class='fg'><label>Name</label><input name='name' placeholder='Full name' required/></div>
                  <div class='fg'><label>Phone</label><input name='phone' placeholder='Phone number' required/></div>
                  <div class='fg'><label>Vehicle Type</label>
                    <select name='vehicle_type'>
                      <option>Bicycle</option><option>Motorbike</option><option>Scooter</option><option>Car</option>
                    </select>
                  </div>
                </div>
                <div class='modal-foot'>
                  <button type='button' class='btn' onclick="closeModal('add-partner-modal')" style='background:var(--surface2)'>Cancel</button>
                  <button type='submit' class='btn btn-primary'>Save</button>
                </div>
              </form>
            </div></div>
            <div id='edit-partner-modal' class='modal-bg'><div class='modal'>
              <h3>Edit Partner</h3>
              <form method='POST' action='/partners' id='edit-partner'>
                <input type='hidden' name='action' value='update'/>
                <input type='hidden' name='id'/>
                <div class='form-row'>
                  <div class='fg'><label>Name</label><input name='name' required/></div>
                  <div class='fg'><label>Phone</label><input name='phone' required/></div>
                  <div class='fg'><label>Vehicle</label>
                    <select name='vehicle_type'>
                      <option>Bicycle</option><option>Motorbike</option><option>Scooter</option><option>Car</option>
                    </select>
                  </div>
                </div>
                <div class='modal-foot'>
                  <button type='button' class='btn' onclick="closeModal('edit-partner-modal')" style='background:var(--surface2)'>Cancel</button>
                  <button type='submit' class='btn btn-primary'>Update</button>
                </div>
              </form>
            </div></div>
            """;
            send(ex, wrap("partners","Delivery Partners",body));
        }
    }

    static class StaticHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            ex.sendResponseHeaders(404, -1);
        }
    }
}
