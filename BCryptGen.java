import java.security.MessageDigest;

public class BCryptGen {
    // Minimal BCrypt implementation to generate/verify
    public static void main(String[] args) throws Exception {
        String password = "admin123";
        // Use Spring Security BCrypt jar from Maven repo
        // Instead, call mvn directly - this class finds the spring-security jar
        System.out.println("Testing BCrypt hash for: " + password);
        
        // Generate using reflection to load BCryptPasswordEncoder from classpath
        String classPath = System.getProperty("java.class.path");
        System.out.println("Classpath: " + classPath);
    }
}
