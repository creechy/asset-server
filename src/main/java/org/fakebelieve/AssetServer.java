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
            // Since this example is for our test webapp, we need to setup a
            // LoginService so this shows how to create a very simple hashmap based
            // one. The name of the LoginService needs to correspond to what is
            // configured a webapp's web.xml and since it has a lifecycle of its own
            // we register it as a bean with the Jetty server object so it can be
            // started and stopped according to the lifecycle of the server itself.
            // In this example the name can be whatever you like since we are not
            // dealing with webapp realms.
            LoginService loginService = new HardcodedLoginService(username, password);
            server.addBean(loginService);

            // A security handler is a jetty handler that secures content behind a
            // particular portion of a url space. The ConstraintSecurityHandler is a
            // more specialized handler that allows matching of urls to different
            // constraints. The server sets this as the first handler in the chain,
            // effectively applying these constraints to all subsequent handlers in
            // the chain.
            ConstraintSecurityHandler security = new ConstraintSecurityHandler();
            server.setHandler(security);

            // This constraint requires authentication and in addition that an
            // authenticated user be a member of a given set of roles for
            // authorization purposes.
            Constraint constraint = new Constraint();
            constraint.setName("auth");
            constraint.setAuthenticate(true);
            constraint.setRoles(new String[]{"user"});

            // Binds a url pattern with the previously created constraint. The roles
            // for this constraing mapping are mined from the Constraint itself
            // although methods exist to declare and bind roles separately as well.
            ConstraintMapping mapping = new ConstraintMapping();
            mapping.setPathSpec("/*");
            mapping.setConstraint(constraint);

            // First you see the constraint mapping being applied to the handler as
            // a singleton list, however you can passing in as many security
            // constraint mappings as you like so long as they follow the mapping
            // requirements of the servlet api. Next we set a BasicAuthenticator
            // instance which is the object that actually checks the credentials
            // followed by the LoginService which is the store of known users, etc.
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