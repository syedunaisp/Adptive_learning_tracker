import java.sql.*;

public class TestMath2 {
    public static void main(String[] args) throws Exception {
        System.out.println("Connecting to alip_data.db...");
        Connection c = DriverManager.getConnection("jdbc:sqlite:alip_data.db");
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT * FROM students");
        int count = 0;
        while (rs.next()) {
            System.out.println("Found student: " + rs.getString("name"));
            count++;
        }
        System.out.println("Total students: " + count);
        rs.close();
        s.close();
        c.close();
    }
}
