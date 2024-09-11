import com.ontotext.graphdb.replaytool.goreplay.GoReplayPackage;
import com.ontotext.graphdb.replaytool.goreplay.RDF4JPackage;
import org.junit.Test;

import static org.junit.Assert.*;

public class RDF4JPackageTest {

    // Response to a "begin transaction" request. Contains Location header with a transaction ID
    String RES_WITH_TRANSACTION = "32206561383331633230376630303030303130626262303166312031373234343135353831383237353"+
            "63930303020300a485454502f312e3120323031200d0a566172793a204163636570742d456e636f64696e670d0a43616368652d43"+
            "6f6e74726f6c3a206e6f2d73746f72650d0a4c6f636174696f6e3a20687474703a2f2f6c6f63616c686f73743a373230302f72657"+
            "06f7369746f726965732f7374726573732d746573742f7472616e73616374696f6e732f62333961376436612d386338312d343232"+
            "332d616332392d6564333236343334396162330d0a436f6e74656e742d547970653a20746578742f706c61696e3b6368617273657"+
            "43d5554462d380d0a436f6e74656e742d4c616e67756167653a20656e2d55530d0a436f6e74656e742d4c656e6774683a20300d0a"+
            "446174653a204672692c2032332041756720323032342031323a31393a343120474d540d0a4b6565702d416c6976653a2074696d6"+
            "56f75743d36300d0a436f6e6e656374696f6e3a206b6565702d616c6976650d0a5365727665723a20477261706844422f31302e37"+
            "2d534e415053484f5420524446344a2f342e332e31330d0a0d0a";
    String RES_TRANSACTION_ID = "b39a7d6a-8c81-4223-ac29-ed3264349ab3";
    String RES_NEW_TRANSACTION_ID = "c40b8eeb-9d92-5334-bd30-fe4375450bc4";
    String RES_ID = "ea831c207f0000010bbb01f1";
    // "Begin transaction" request
    String REQ_NO_TRANSACTION = "3120656138333163323037663030303030313062626230316631203137323434313535383138323536363"+
            "230303020300a504f5354202f7265706f7369746f726965732f7374726573732d746573742f7472616e73616374696f6e73204854"+
            "54502f312e310d0a436f6e74656e742d547970653a206170706c69636174696f6e2f782d7777772d666f726d2d75726c656e636f6"+
            "465643b20636861727365743d7574662d380d0a436f6e74656e742d4c656e6774683a20300d0a486f73743a206c6f63616c686f73"+
            "743a373230300d0a436f6e6e656374696f6e3a204b6565702d416c6976650d0a557365722d4167656e743a204170616368652d487"+
            "47470436c69656e742f342e352e313220284a6176612f32322e302e32290d0a4163636570742d456e636f64696e673a20677a6970"+
            "2c6465666c6174650d0a0d0a";
    String REQ_ID = "ea831c207f0000010bbb01f1";
    RDF4JPackage rdf4JPackage;


    @Test
    public void responseWithTransaction() {
        rdf4JPackage = new RDF4JPackage(RES_WITH_TRANSACTION);
        assertEquals("Package ID does not match", rdf4JPackage.getId(), RES_ID);
        assertEquals("Transaction ID mismatch", rdf4JPackage.getCompoundTransaction(), RES_TRANSACTION_ID);
        assertEquals("Payload modified", rdf4JPackage.getPayload(), RES_WITH_TRANSACTION);
        rdf4JPackage.replaceCompoundTransaction(RES_NEW_TRANSACTION_ID);
        rdf4JPackage = new RDF4JPackage(rdf4JPackage.getPayload());
        assertEquals("New transaction ID mismatch", rdf4JPackage.getCompoundTransaction(), RES_NEW_TRANSACTION_ID);
    }

    @Test
    public void requestWithoutTransaction() {
        rdf4JPackage = new RDF4JPackage(REQ_NO_TRANSACTION);
        assertEquals("Package ID does not match", rdf4JPackage.getId(), REQ_ID);
        assertNull("Bogus transaction ID", rdf4JPackage.getCompoundTransaction());
        assertFalse("Package is not part of a compound transaction", rdf4JPackage.isCompoundTransaction());
        assertEquals("Incorrect payload type, should be 1", rdf4JPackage.getType(), GoReplayPackage.PACKAGE_TYPE_REQUEST);
        rdf4JPackage.replaceCompoundTransaction(RES_NEW_TRANSACTION_ID);
        assertEquals("Payload should be unchanged", rdf4JPackage.getPayload(), REQ_NO_TRANSACTION);
    }
}
