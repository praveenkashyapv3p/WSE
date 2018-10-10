package servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class Jsonservlet extends HttpServlet {


    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/json;charset=UTF-8");
        boolean dispjson = true;
        try {
            String queryrequest = request.getParameter("query");
            String input_k = request.getParameter("k");
            String languag = request.getParameter("lang");
            String model = request.getParameter("score");
            String image = request.getParameter("image");
            PrintWriter writer = response.getWriter();
            Resultservlet resultservlet = new Resultservlet();
            resultservlet.getClientIp(request);

            resultservlet.queryprocessing(queryrequest, input_k, image, languag,model, writer, request, response,dispjson);
        } catch (Exception ex) {
            System.out.println("Catch block: Servlet Exception");
            ex.printStackTrace();
        }
    }
}
