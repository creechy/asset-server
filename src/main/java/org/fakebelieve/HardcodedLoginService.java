package org.fakebelieve;


import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HardcodedLoginService implements LoginService {
    private final static Logger log = LoggerFactory.getLogger(HardcodedLoginService.class);

    private final Map users = new ConcurrentHashMap();

    // matches what is in the constraint object in the spring config
    private final String[] ACCESS_ROLE = new String[]{"user"};
    private final String username;
    private final String password;

    public HardcodedLoginService(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private IdentityService identityService = new DefaultIdentityService();

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    @Override
    public String getName() {
        return "";
    }

    @Override
    public UserIdentity login(final String username, Object creds, ServletRequest servletRequest) {


        UserIdentity user = null;


        // HERE IS THE HARDCODING
        boolean validUser = this.username.equals(username) && this.password.equals(creds);
        if (validUser) {
//            Credential credential = (creds instanceof Credential) ? (Credential) creds : Credential.getCredential(creds.toString());

            Principal userPrincipal = new Principal() {

                @Override
                public String getName() {
                    return username;
                }
            };
            Subject subject = new Subject();
            subject.getPrincipals().add(userPrincipal);
            subject.getPrivateCredentials().add(creds);
            subject.setReadOnly();
            user = identityService.newUserIdentity(subject, userPrincipal, ACCESS_ROLE);
            users.put(user.getUserPrincipal().getName(), true);

            log.info("Logging in \"{}\"", username);
        }

        return user;
    }

    @Override
    public void logout(UserIdentity arg0) {

    }

    @Override
    public void setIdentityService(IdentityService arg0) {
        this.identityService = arg0;

    }

    @Override
    public boolean validate(UserIdentity user) {
        return users.containsKey(user.getUserPrincipal().getName());
    }

}
