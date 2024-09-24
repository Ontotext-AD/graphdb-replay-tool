import com.ontotext.graphdb.replaytool.goreplay.GraphDBPackage;
import org.junit.Before;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class GraphDBPackageTest {

    final static String AUTHENTICATION_SECRET = "GraphDBRulezBigTime";

    final String REQ_WITH_GDB_AUTHENTICATION =
                    "504f5354202f7265706f7369746f726965732f7374726573732d7465737420485454502f312e310a" +
                    "4163636570743a206170706c69636174696f6e2f782d73706172716c737461722d726573756c7473" +
                    "2b6a736f6e2c206170706c69636174696f6e2f73706172716c2d726573756c74732b6a736f6e3b71" +
                    "3d302e392c202a2f2a3b713d302e380a4163636570742d456e636f64696e673a20677a69702c2064" +
                    "65666c6174652c2062722c207a7374640a4163636570742d4c616e67756167653a20656e2d55532c" +
                    "656e3b713d302e390a417574686f72697a6174696f6e3a204744422065794a3163325679626d4674" +
                    "5a534936496d466b62576c7549697769595856306147567564476c6a5958526c5a454630496a6f78" +
                    "4e7a49314f5463334e4451354f44593366513d3d2e68362b78696469354a6f714243306f37304f30" +
                    "777a325055476376746d387a7654785837643066374278343d0a436f6e74656e742d4c656e677468" +
                    "3a203133300a436f6e74656e742d547970653a206170706c69636174696f6e2f782d7777772d666f" +
                    "726d2d75726c656e636f6465640a582d477261706844422d43617463683a20313030303b20746872" +
                    "6f770a582d477261706844422d4c6f63616c2d436f6e73697374656e63793a207570646174696e67" +
                    "0a0a71756572793d73656c6563742532302a25323077686572652532302537422530412532302532" +
                    "3025323025323025334673253230253346702532302533466f2532302e2530412537442532306c69" +
                    "6d697425323031303026696e6665723d747275652673616d6541733d74727565266f66667365743d" +
                    "30266c696d69743d313030310a";
    final String REQ_WITH_BASIC_AUTHENTICATION =
                    "504f5354202f7265706f7369746f726965732f7374726573732d7465737420485454502f312e310a" +
                    "4163636570743a206170706c69636174696f6e2f782d73706172716c737461722d726573756c7473" +
                    "2b6a736f6e2c206170706c69636174696f6e2f73706172716c2d726573756c74732b6a736f6e3b71" +
                    "3d302e392c202a2f2a3b713d302e380a4163636570742d456e636f64696e673a20677a69702c2064" +
                    "65666c6174652c2062722c207a7374640a4163636570742d4c616e67756167653a20656e2d55532c" +
                    "656e3b713d302e390a417574686f72697a6174696f6e3a2042617369632059575274615734366347" +
                    "467a63336476636d513d0a436f6e74656e742d4c656e6774683a203133300a436f6e74656e742d54" +
                    "7970653a206170706c69636174696f6e2f782d7777772d666f726d2d75726c656e636f6465640a58" +
                    "2d477261706844422d43617463683a20313030303b207468726f770a582d477261706844422d4c6f" +
                    "63616c2d436f6e73697374656e63793a207570646174696e670a0a71756572793d73656c65637425" +
                    "32302a25323077686572652532302537422530412532302532302532302532302533467325323025" +
                    "3346702532302533466f2532302e2530412537442532306c696d697425323031303026696e666572" +
                    "3d747275652673616d6541733d74727565266f66667365743d30266c696d69743d313030310a";
    final String REQ_WITHOUT_AUTHENTICATION =
                    "504f5354202f7265706f7369746f726965732f7374726573732d7465737420485454502f312e310a" +
                    "4163636570743a206170706c69636174696f6e2f782d73706172716c737461722d726573756c7473" +
                    "2b6a736f6e2c206170706c69636174696f6e2f73706172716c2d726573756c74732b6a736f6e3b71" +
                    "3d302e392c202a2f2a3b713d302e380a4163636570742d456e636f64696e673a20677a69702c2064" +
                    "65666c6174652c2062722c207a7374640a4163636570742d4c616e67756167653a20656e2d55532c" +
                    "656e3b713d302e390a436f6e74656e742d4c656e6774683a203133300a436f6e74656e742d547970" +
                    "653a206170706c69636174696f6e2f782d7777772d666f726d2d75726c656e636f6465640a582d47" +
                    "7261706844422d43617463683a20313030303b207468726f770a582d477261706844422d4c6f6361" +
                    "6c2d436f6e73697374656e63793a207570646174696e670a0a71756572793d73656c656374253230" +
                    "2a253230776865726525323025374225304125323025323025323025323025334673253230253346" +
                    "702532302533466f2532302e2530412537442532306c696d697425323031303026696e6665723d74" +
                    "7275652673616d6541733d74727565266f66667365743d30266c696d69743d313030310a";

    GraphDBPackage graphDBPackage;

    @Before
    public void setUp() throws NoSuchAlgorithmException, InvalidKeyException {
        GraphDBPackage.setAuthorizationSecret(AUTHENTICATION_SECRET);
    }

    @Test
    public void testPackageWithGdbAuthentication() throws InvalidKeyException {
        graphDBPackage = new GraphDBPackage(REQ_WITH_GDB_AUTHENTICATION);
        assertTrue("Authorization not detected", graphDBPackage.usesAuthorization());
        assertEquals("Authorization type incorrect", GraphDBPackage.AUTHORIZATION_TYPE_GDB,
                graphDBPackage.getAuthorizationType());
        assertEquals("Authorization user incorrect", "admin", graphDBPackage.getAuthorizationUser());
        graphDBPackage.replaceAuthorizationToken();
        assertTrue("Payload should be modified", graphDBPackage.modified());
        assertNotEquals("Payload should have changed", REQ_WITH_GDB_AUTHENTICATION, graphDBPackage.getPayload());
        graphDBPackage = new GraphDBPackage(graphDBPackage.getPayload());
        assertEquals("Authorization user incorrect", "admin", graphDBPackage.getAuthorizationUser());
        assertEquals("Authorization type incorrect", GraphDBPackage.AUTHORIZATION_TYPE_GDB,
                graphDBPackage.getAuthorizationType());
    }

    @Test
    public void testPackageWithBasicAuthentication() throws InvalidKeyException {
        graphDBPackage = new GraphDBPackage(REQ_WITH_BASIC_AUTHENTICATION);
        assertTrue("Authorization not detected", graphDBPackage.usesAuthorization());
        assertEquals("Authorization type incorrect", GraphDBPackage.AUTHORIZATION_TYPE_BASIC,
                graphDBPackage.getAuthorizationType());
        assertEquals("Authorization user incorrect", "admin", graphDBPackage.getAuthorizationUser());
        graphDBPackage.replaceAuthorizationToken();
        assertTrue("Payload should be modified", graphDBPackage.modified());
        assertNotEquals("Payload should have changed", REQ_WITH_GDB_AUTHENTICATION, graphDBPackage.getPayload());
        graphDBPackage = new GraphDBPackage(graphDBPackage.getPayload());
        assertEquals("Authorization user incorrect", "admin", graphDBPackage.getAuthorizationUser());
        assertEquals("Authorization type incorrect", GraphDBPackage.AUTHORIZATION_TYPE_GDB,
                graphDBPackage.getAuthorizationType());
    }

    @Test
    public void testPackageWithoutAuthentication() throws InvalidKeyException {
        graphDBPackage = new GraphDBPackage(REQ_WITHOUT_AUTHENTICATION);
        assertFalse("Authorization not detected", graphDBPackage.usesAuthorization());
        assertEquals("Authorization type incorrect", GraphDBPackage.AUTHORIZATION_TYPE_NONE,
                graphDBPackage.getAuthorizationType());
        graphDBPackage.replaceAuthorizationToken();
        assertFalse("Payload should not be modified", graphDBPackage.modified());
        assertEquals("Payload should have changed", REQ_WITHOUT_AUTHENTICATION, graphDBPackage.getPayload());
    }
}
