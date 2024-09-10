import com.ontotext.graphdb.replaytool.goreplay.GraphDBPackage;
import org.junit.Before;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class GraphDBPackageTest {

    final static String AUTHENTICATION_SECRET = "GraphDBRulezBigTime";

    GraphDBPackage graphDBPackage;

    @Before
    public void setUp() throws NoSuchAlgorithmException, InvalidKeyException {
        GraphDBPackage.setAuthorizationSecret(AUTHENTICATION_SECRET);
    }

    @Test
    public void testPackageWithoutAuthentication() {

    }
}
