package app; // לשים את שם ה-package הנכון אצלך

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DerbyTest {
    public static void main(String[] args) {
        try {
            // יצירת חיבור למסד נתונים בשם testDB (נשמר כתיקייה בפרויקט שלך)
            Connection conn = DriverManager.getConnection("jdbc:derby:testDB;create=true");

            Statement stmt = conn.createStatement();

            // יצירת טבלה (רק אם לא קיימת)
            try {
                stmt.executeUpdate("CREATE TABLE Users (id INT PRIMARY KEY, name VARCHAR(50))");
                System.out.println("טבלה Users נוצרה בהצלחה.");
            } catch (Exception e) {
                System.out.println("הטבלה כבר קיימת, ממשיכים...");
            }

            // הוספת שורה
            stmt.executeUpdate("INSERT INTO Users VALUES (1, 'Polina')");

            // שליפה (SELECT)
            ResultSet rs = stmt.executeQuery("SELECT * FROM Users");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("id") + ", Name: " + rs.getString("name"));
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
