import io.javalin.Javalin;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.sql.ResultSet;

public class UrlShortenerApp {

    // The Base62 character set for URL-safe codes
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            // 1. Servir les fichiers HTML/CSS/JS depuis src/main/resources/public
            config.staticFiles.add("/public");

            // 2. Nouvelle syntaxe Javalin 6 pour activer CORS
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        }).start(8080);
        // POST Endpoint: Accepts a raw URL string in the request body
        app.post("/shorten", ctx -> {
            String longUrl = ctx.body();

            if (longUrl == null || longUrl.trim().isEmpty()) {
                ctx.status(400).result("Error: Please provide a valid URL in the request body.");
                return;
            }

            String shortCode = generateShortCode(6);

            // Execute the JDBC Insert
            String sql = "INSERT INTO urls (short_code, long_url) VALUES (?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, shortCode);
                pstmt.setString(2, longUrl);
                pstmt.executeUpdate();

                // Return the final shortened URL to the user
                String finalUrl = "http://localhost:8080/" + shortCode;
                ctx.status(201).result(finalUrl);

            } catch (SQLException e) {
                // If the 6-character code already exists, Postgres will throw a unique constraint violation.
                // In a production app, you would loop and retry here. For tonight, just print the error.
                e.printStackTrace();
                ctx.status(500).result("Database error while saving the URL.");
            }
        });

        /*
        app.get("/", ctx -> {
            ctx.result("URL Shortener is running! Send a POST request to /shorten to create a link.");
        });*/

        // 2. The main Redirect Endpoint
        app.get("/{shortCode}", ctx -> {
            // Grab the short code directly from the URL path (e.g., from localhost:8080/x7B2pM)
            String shortCode = ctx.pathParam("shortCode");

            // Query the database to find the matching long URL
            String sql = "SELECT long_url FROM urls WHERE short_code = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, shortCode);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String longUrl = rs.getString("long_url");
                    // Javalin handles the HTTP 302 redirect automatically with this method
                    ctx.redirect(longUrl);
                } else {
                    // If the code isn't in the database
                    ctx.status(404).result("Short URL not found.");
                }

            } catch (SQLException e) {
                e.printStackTrace();
                ctx.status(500).result("Database error during redirect.");
            }
        });

    }

    // Helper function to generate the random Base62 string
    private static String generateShortCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(randomIndex));
        }
        return sb.toString();
    }
}