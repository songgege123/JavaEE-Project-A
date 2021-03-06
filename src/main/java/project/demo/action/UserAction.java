package project.demo.action;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.jasypt.util.password.StrongPasswordEncryptor;
import project.demo.model.User;
import project.demo.util.DB;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
                        resultSet.getString("password"),
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
                return;
            }
        }
        req.setAttribute("message", "Invalid Email or password.");
        req.getRequestDispatcher("sign-in.jsp").forward(req, resp);
    }

    private void signUp(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = null;
        String username = null;
        String password = null;
        String avatar = "default.png";

        DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
        ServletContext servletContext = req.getServletContext();
        String attribute = "javax.servlet.context.tempdir";
        File repository = (File) servletContext.getAttribute(attribute);
        diskFileItemFactory.setRepository(repository);
        ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);

        try {
            List<FileItem> fileItems = servletFileUpload.parseRequest(req);
            for (FileItem fileItem : fileItems) {
                if (fileItem.isFormField()) {
                    // fileItem 是普通的属性
                    switch (fileItem.getFieldName()) {
                        case "email":
                            email = fileItem.getString();
                            if (queryUserByEmail(email) != null) {
                                req.setAttribute("message", "Email is existed.");
                                req.getRequestDispatcher("sign-up.jsp").forward(req, resp);
                                return;
                            }
                            break;
                        case "username":
                            username = fileItem.getString("UTF-8");
                            break;
                        case "password":
                            password = fileItem.getString();
                            StrongPasswordEncryptor strongPasswordEncryptor = new StrongPasswordEncryptor();
                            password = strongPasswordEncryptor.encryptPassword(password);
                            break;
                        default:
                            break;
                    }
                } else {
                    // fileItem 是上传的文件
                    // TODO: 10/10/2018
//                    fileItem.getContentType(); // Image/gif
//                    fileItem.getSize(); // size

                    String originName = fileItem.getName();
                    if (!originName.isEmpty()) {
                        String extension = originName.substring(originName.lastIndexOf("."));
                        String fileName = System.nanoTime() + extension;
                        avatar = fileName;
                        // 保存到服务器
                        File file = new File(servletContext.getRealPath("/avatar") + "/" + fileName);
                        fileItem.write(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
