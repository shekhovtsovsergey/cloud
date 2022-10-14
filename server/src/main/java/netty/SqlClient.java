package netty;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

public class SqlClient {
    private static Connection connection;
    private static Statement statement;

    public synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:clients-db.sqlite");
            statement = connection.createStatement();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            connection.close();
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    synchronized static String getNick(String login, String password) {
        String query = String.format(
                "select nickname from users where login='%s' and password='%s'",
                login, password);
        try (ResultSet set = statement.executeQuery(query)) {
            if (set.next())
                return set.getString("nickname");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }


    synchronized static List<String> getUsers() {

        List ll = new LinkedList();
        String query = String.format("select nickname from users");
        try (ResultSet set = statement.executeQuery(query)) {
            while (set.next()) {
                String str = set.getString("nickname");
                //ll.add(0, "All");
                ll.add(str);
                System.out.println(str);
            }
            return ll;
            //System.out.println(ll);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
