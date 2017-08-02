package org.fakebelieve;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AssetServer extends AbstractHandler {

    private static final Logger logger = LoggerFactory.getLogger(AssetServer.class);

    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm");
    private DecimalFormat decimalFormat = new DecimalFormat("#,###");
    private List<String> files;

    public AssetServer(List<String> files) {
        this.files = files;
    }

    @Override
    public void handle(String target,
                       Request baseRequest,
                       HttpServletRequest request,
                       HttpServletResponse response) throws IOException,
            ServletException {

        String idxParam = request.getParameter("idx");

        if (idxParam == null) {

            // Declare response encoding and types
            response.setContentType("text/html; charset=utf-8");

            // Declare response status code
            response.setStatus(HttpServletResponse.SC_OK);

            // Write back response

            PrintWriter writer = response.getWriter();
            writer.println("<html>");
            writer.println("<head><title>Asset Server</title></head>");
            writer.println("<body>");

            writer.println("<br/>");

            writer.println("<table border=\"0\">");
            for (int idx = 0; idx < files.size(); idx++) {
                String file = files.get(idx);
                File asset = new File(file);
                Date modified = new Date(asset.lastModified());

                writer.print("<tr>");
                writer.print("<td><a href=\"/?idx=" + idx + "\"><span style=\"font-family: monospace\">" + file + "</span></a></td>");
                writer.print("<td>" + dateFormat.format(modified) + "</td>");
                writer.print("<td>(" + decimalFormat.format(asset.length()) + " bytes)</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");

            writer.println("<br/>");
            writer.println("[" + files.size() + " file(s)]");

            writer.println("</body>");
            writer.println("</html>");

            // Inform jetty that this request has now been handled
            baseRequest.setHandled(true);

            logger.info("Served listing.");
        } else {

            int idx = Integer.parseInt(idxParam);
            String file = files.get(idx);
            File asset = new File(file);

            // Declare response encoding and types
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "filename=\"" + file + "\"");
            response.setHeader("Content-Length", Long.toString(asset.length()));

            // Declare response status code
            response.setStatus(HttpServletResponse.SC_OK);

            BufferedInputStream reader = new BufferedInputStream(new FileInputStream(file));
            ServletOutputStream writer = response.getOutputStream();


            IOUtils.copy(reader, writer);

            IOUtils.closeQuietly(reader);

            // Inform jetty that this request has now been handled
            baseRequest.setHandled(true);

            logger.info("Served file \"" + file + "\"");
        }
    }

    public static void main(String[] args) throws Exception {

        int port = 8080;
        List<String> files = new ArrayList<>();

        for (int idx = 0; idx < args.length; idx++) {
            if (args[idx].equals("--port")) {
                port = Integer.parseInt(args[++idx]);
                continue;
            }

            if (args[idx].equals("--help")) {
                System.out.println("args: [--port <port-number>] [file1 .. fileN]");
                return;
            }

            files.add(args[idx]);
        }

        Server server = new Server(port);

        AssetServer assetServer = new AssetServer(files);
        server.setHandler(assetServer);

        server.start();
        server.join();
    }
}