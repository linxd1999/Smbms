package cn.smbms.servlet;

import cn.smbms.entity.PageBean;
import cn.smbms.entity.ResultInfo;
import cn.smbms.entity.User;
import cn.smbms.service.impl.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author lxd
 * @create 2020-07-09 21:03
 */
@WebServlet("/user/*")
public class UserServlet extends BaseServlet{

    private UserServiceImpl service = new UserServiceImpl();
    /**
     * 登录功能
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void login(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("utf-8");
        resp.setContentType("text/html;utf-8");
        //获取请求参数
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        String checkcode = req.getParameter("checkcode");
        System.out.println(new String(new char[10])+"a");
        //先判断验证码是否正确
        HttpSession session = req.getSession();
        String checkcode_server = (String) session.getAttribute("CHECKCODE_SERVER");
        session.removeAttribute("CHECKCODE_SERVER");
        //验证码忽略大小写
        ResultInfo info = new ResultInfo();
        if (checkcode_server !=null && checkcode_server.equalsIgnoreCase(checkcode)){
            //验证码正确
            //判断用户名和密码是否正确
            //封装user对象
            User loginUser = new User();
            loginUser.setUsername(username);
            loginUser.setPassword(password);
//            UserServiceImpl service = new UserServiceImpl();
            User user = service.login(loginUser);
            //响应结果给前端
            if (user == null){
                info.setFlag(false);
                info.setErrorMsg("用户名或密码错误");
            }else {
                //给前端发送登录成功的信息
                req.getSession().setAttribute("user",user);
                info.setFlag(true);
                info.setData(user);
            }
        }else {
            //存储提示信息到request
            info.setFlag(false);
            info.setErrorMsg("验证码错误");
            //转发到登录页面
        }
        //将info对象序列化为json
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(info);
        //将json数据写回客户端
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(json);
    }

    /**
     * 退出功能
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void exit(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getSession().invalidate();
        resp.sendRedirect(req.getContextPath()+"/login.html");
    }


    /**
     * 查询所有用户
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void findAll(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        UserServiceImpl service = new UserServiceImpl();
        List<User> users = service.findAll();
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(users);
        System.out.println(json);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(json);
    }

    /**
     * 根据登录的用户查询其用户名
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void findOne(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //从session中取出用户
        Object user = req.getSession().getAttribute("user");
        //将用户写回客户端
        ObjectMapper mapper = new ObjectMapper();
        resp.setContentType("application/json;charset=utf-8");
        mapper.writeValue(resp.getOutputStream(),user);
    }

    /**
     * 删除单条目
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void delete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        service.deleteUser(id);
        resp.sendRedirect(req.getContextPath()+"/userList.html");
    }

    /**
     * 删除选中条目
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void deleteSelected(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        String[] ids = req.getParameterValues("uid");
        service.deleteSelectedUser(ids);
        resp.sendRedirect(req.getContextPath()+"/userList.html");
    }

    /**
     * 分页查询数据
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void findByPage(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException{
        String currentPage = req.getParameter("currentPage");
        String rows = req.getParameter("rows");

        Map<String, String[]> condition = req.getParameterMap();

        PageBean<User> pb = service.findUserByPage(currentPage,rows,condition);
        ObjectMapper mapper = new ObjectMapper();
        resp.setContentType("application/json;charset=utf-8");
        mapper.writeValue(resp.getOutputStream(),pb);
    }

    /**
     * 修改用户密码
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void modifiedPWD(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("user");

        String oldpassword = req.getParameter("oldpassword");
        String newpassword = req.getParameter("newpassword");
        String confirmpassword = req.getParameter("confirmpassword");
        //System.out.println("old-" + oldpassword + " new-" + newpassword + " confirm-" + confirmpassword);

        ResultInfo info = new ResultInfo();
        String errorMsg = "抱歉，密码修改失败！";

        if (oldpassword == null || newpassword == null || confirmpassword == null || oldpassword.isEmpty() || newpassword.isEmpty() || confirmpassword.isEmpty() || !oldpassword.equals(user.getPassword()) || oldpassword.equals(newpassword) || !newpassword.equals(confirmpassword)) {
            //未成功修改
            info.setFlag(false);
            if (oldpassword == null || newpassword == null || confirmpassword == null || oldpassword.isEmpty() || newpassword.isEmpty() || confirmpassword.isEmpty()) {
                errorMsg += "请不要保留空白项";
            } else if (!oldpassword.equals(user.getPassword())) {
                errorMsg += "旧密码输入不正确";
            } else if (oldpassword.equals(newpassword)) {
                errorMsg += "新密码不可与旧密码相同";
            } else {
                errorMsg += "两次密码输入不一致";
            }
            info.setErrorMsg(errorMsg);

        } else {
            //成功修改
            service.modifiedPWD(newpassword, user.getId());
            info.setFlag(true);

            //更新session对象中的user对象
            user.setPassword(newpassword);
            //System.out.println(((User)req.getSession().getAttribute("user")).getPassword());
        }
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(info);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(json);
    }

    /**
     * 将待修改用户信息写入响应消息
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void findAnother(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User targetUser = (User) req.getSession().getAttribute("targetUser");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(targetUser);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(json);
    }

    /**
     * 跳转到修改信息页面，并设置待修改User对象
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void forwardModifiedInfo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String s_id = req.getParameter("id");
        User targetUser = null;
        if (s_id != null && s_id != "")
            targetUser = service.findById(Integer.parseInt(s_id));

        req.getSession().setAttribute("targetUser", targetUser);
        resp.sendRedirect(req.getContextPath() + "/modifiedUserInfo.html");

    }

    /**
     * 修改用户信息
     *
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    public void modifiedInfo(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String username = req.getParameter("username");
        String gender = req.getParameter("gender");
        String phone = req.getParameter("phone");
        String email = req.getParameter("email");
        String userimage = req.getParameter("userimage");
        String imagename = req.getParameter("imagename");
        System.out.println("头像："  + userimage);
        System.out.println(imagename);

        User targetUser = (User) req.getSession().getAttribute("targetUser");

        String phoneRegex = "^1[3456789]\\d{9}$";
        String emailRegex = "\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*";

        String errorMsg = "抱歉，信息修改失败！";
        ResultInfo info = new ResultInfo();
        //检验正确性
        if (username == null || username == "" || gender == null || gender == "" || phone == null || phone == "" || email == null || email == "") {
            errorMsg += "请不要保留空白项";
            info.setErrorMsg(errorMsg);
            info.setFlag(false);
        } else if (username.equals(targetUser.getUsername()) && gender.equals(targetUser.getGender()) && phone.equals(targetUser.getPhone()) && email.equals(targetUser.getEmail()) && (imagename == null || "".equals(imagename))) {
            //信息未发生修改
            errorMsg += "信息未修改";
            info.setErrorMsg(errorMsg);
            info.setFlag(false);
        } else if(!Pattern.matches(phoneRegex, phone)){
            //手机号码不合法
            errorMsg += "输入的电话号码不合法";
            info.setErrorMsg(errorMsg);
            info.setFlag(false);
        } else if(!Pattern.matches(emailRegex, email)){
            errorMsg += "输入的电子邮箱地址不合法";
            info.setFlag(false);
            info.setErrorMsg(errorMsg);
        } else if(imagename != null && !"".equals(imagename) && !imagename.contains(".jpg")){
            errorMsg += "暂时只支持jpg格式头像图片";
            info.setFlag(false);
            info.setErrorMsg(errorMsg);
        } else {

            //修改头像，只有上传了头像的情况下才会修改，不然不会修改原头像
            if(userimage != null && !"".equals(userimage)){
                if(userimage.startsWith("data:")){
                    String[] strings = userimage.split(",");
                    String head = strings[0];
                    userimage = strings[1];
                }
                userimage = userimage.replace("\r\n", "");
                byte[] bytes = Base64.getDecoder().decode(userimage.replaceAll(" ", "+"));
                System.out.println(Arrays.toString(bytes));
                String fileName = req.getServletContext().getRealPath("image") + "\\" + "userimages\\"+ targetUser.getId() + ".jpg";
                System.out.println("文件名：" + fileName);
                File file = new File(fileName);
                File dir = file.getParentFile();
                if(dir == null || !dir.exists()){
                    System.out.println(dir.mkdirs());
                }
                OutputStream out = null;
                try {
                    out = new FileOutputStream(file);
                    out.write(bytes);
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //修改成功
            targetUser.setUsername(username);
            targetUser.setGender(gender);
            targetUser.setPhone(phone);
            targetUser.setEmail(email);

            service.modifiedInfo(targetUser);
            info.setFlag(true);

            if (targetUser.getId() == ((User) (req.getSession().getAttribute("user"))).getId()) {
                //修改的是当前登录用户的信息，更新session域中的user
                req.getSession().setAttribute("user", targetUser);
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(info);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write(json);
    }
}
