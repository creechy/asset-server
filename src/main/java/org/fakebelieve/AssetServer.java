package org.fakebelieve;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.security.Constraint;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Simple HTTP server to provide download access to assets
 * <p>
 * This is a simple embedded Jetty HTTP server that provides the ability to specify files
 * to make available for download. When started, the server will provide a listing of
 * all the registered files with the ability to click on and download each file.
 * <p>
 * Options include the ability to specify a port number and/or simple authentication.
 */

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
        String username = null;
        String password = null;

        for (int idx = 0; idx < args.length; idx++) {
            if (args[idx].equals("--port")) {
                port = Integer.parseInt(args[++idx]);
                continue;
            }

            if (args[idx].equals("--creds")) {
                username = args[++idx];
                password = args[++idx];
                continue;
            }

            if (args[idx].equals("--help")) {
                System.out.println("args: [--port <port-number>] [--creds <username> <password>] [file1 .. fileN]");
                return;
            }

            files.add(args[idx]);
        }

        AssetServer assetServer = new AssetServer(files);

        Server server = new Server(port);

        if (username != null) {
            String[] roles = new String[]{"user"};
            // we need to setup a LoginService. This is a very simple custom
            // single user LoginService. And since it has a lifecycle of its own
            // we register it as a bean with the Jetty server object so it can be
            // started and stopped according to the lifecycle of the server itself.
            LoginService loginService = new HardcodedLoginService(username, password, roles, "Asset Server");
            server.addBean(loginService);

            // Create a security handler is a jetty handler to secure content behind a
            // particular portion of a url space.
            ConstraintSecurityHandler security = new ConstraintSecurityHandler();
            server.setHandler(security);

            // This constraint requires authentication and in addition that an
            // authenticated user be a member of a given set of roles for
            // authorization purposes.
            Constraint constraint = new Constraint();
            constraint.setName("auth");
            constraint.setAuthenticate(true);
            constraint.setRoles(roles);

            // Bind a url pattern with the previously created constraint.
            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setPathSpec("/*");
            mapping.setConstraint(constraint);

            // Register the entire authentication context.
            security.setConstraintMappings(Collections.singletonList(mapping));
            security.setAuthenticator(new BasicAuthenticator());
            security.setLoginService(loginService);

            security.setHandler(assetServer);
        } else {
            server.setHandler(assetServer);
        }

        server.start();
        server.join();
    }
}