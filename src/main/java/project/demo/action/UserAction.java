package project.demo.action;

import org.jasypt.util.password.StrongPasswordEncryptor;
import project.demo.model.User;
import project.demo.util.DB;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(urlPatterns = "/user")
public class UserAction extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = req.getParameter("action");
        switch (action) {
            case "signUp":
                signUp(req, resp);
                break;
            case "signIn":
                signIn(req, resp);
                break;
            case "signOut":
                signOut(req, resp);
                break;
            case "checkEmail":
                checkEmail(req, resp);
                break;
            default:
                break;
        }
    }

    private void checkEmail(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email").trim();
        resp.setContentType("application/json, charset=UTF-8");
        String json = "{\"emailExisted\": true}";
        if (queryUserByEmail(email) == null) {
            json = "{\"emailExisted\": false}";
        }
        resp.getWriter().write(json);
    }

    private User queryUserByEmail(String email) {
        Connection connection = DB.getConnection();
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        String sql = "select * from db_a.user where email = ?";

        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, email);
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                return new User(
                        resultSet.getInt("id"),
                        resultSet.getString("email"),
                        resultSet.getString("username"),
                        resultSet.getString("avatar")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DB.close(resultSet, preparedStatement);
        }
        return null;
    }

    private void signOut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().invalidate();
        resp.sendRedirect("index.jsp");
    }

    private void signIn(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email").trim();
        User user = queryUserByEmail(email);

        if (user != null) {
            StrongPasswordEncryptor strongPasswordEncryptor = new StrongPasswordEncryptor();
            String password = req.getParameter("password");
            if (strongPasswordEncryptor.checkPassword(password, user.getPassword())) {
                req.getSession().setAttribute("user", user);
                resp.sendRedirect("home.jsp");
            }
        }
        req.setAttribute("message", "Invalid Email or password.");
        req.getRequestDispatcher("sign-in.jsp").forward(req, resp);
    }

    private void signUp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email").trim();

        if (queryUserByEmail(email) != null) {
            req.setAttribute("message", "Email is existed.");
            req.getRequestDispatcher("sign-up.jsp").forward(req, resp);
            return;
        }

        String username = req.getParameter("username").trim();
        String password = req.getParameter("password");
        StrongPasswordEncryptor strongPasswordEncryptor = new StrongPasswordEncryptor();
        password = strongPasswordEncryptor.encryptPassword(password);
        String avatar = "default.png";

        Connection connection = DB.getConnection();
        PreparedStatement preparedStatement = null;
        String sql = "insert into db_a.user value(null, ?, ?, ?, ?)";
        try {
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, username);
            preparedStatement.setString(3, password);
            preparedStatement.setString(4, avatar);
            preparedStatement.executeUpdate();
            resp.sendRedirect("index.jsp");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DB.close(null, preparedStatement);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doPost(req, resp);
    }
}
