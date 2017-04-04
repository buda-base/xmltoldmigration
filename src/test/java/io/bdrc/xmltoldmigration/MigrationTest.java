package io.bdrc.xmltoldmigration;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

import io.bdrc.xmltoldmigration.MigrationHelpers;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.validation.Validator;


/**
 * Unit test for simple App.
 */
public class MigrationTest 
{
	final String TESTDIR = "src/test/";
	OntModel ontology = null;
	Validator personValidator = null;
	Validator placeValidator = null;
	Validator lineageValidator = null;
	Validator productValidator = null;
	Validator corporationValidator = null;
	Validator workValidator = null;
	Validator officeValidator = null;
	Validator topicValidator = null;
	Validator outlineValidator = null;
	Validator pubinfoValidator = null;
	Validator imagegroupValidator = null;
	Validator scanrequestValidator = null;
	public static final Converter converter = new Converter();
	
	@Before
	public void init() {
	    MigrationHelpers.usecouchdb = false;
		ontology = MigrationHelpers.getOntologyModel();
		personValidator = MigrationHelpers.getValidatorFor("person");
		placeValidator = MigrationHelpers.getValidatorFor("place");
		lineageValidator = MigrationHelpers.getValidatorFor("lineage");
		productValidator = MigrationHelpers.getValidatorFor("product");
		corporationValidator = MigrationHelpers.getValidatorFor("corporation");
		workValidator = MigrationHelpers.getValidatorFor("work");
		officeValidator = MigrationHelpers.getValidatorFor("office");
		topicValidator = MigrationHelpers.getValidatorFor("topic");
		outlineValidator = MigrationHelpers.getValidatorFor("outline");
		pubinfoValidator = MigrationHelpers.getValidatorFor("pubinfo");
		imagegroupValidator = MigrationHelpers.getValidatorFor("imagegroup");
		scanrequestValidator = MigrationHelpers.getValidatorFor("scanrequest");
	}
	
	public void flushLog() {
	    try {
            MigrationHelpers.logWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	@Test
    public void testP1331()
    {
	    System.out.println("testing P1331");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
		assertTrue(CommonMigration.documentValidates(d, personValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1331.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	public String toUnicode(String s, List<String>conversionWarnings) {
	    String convertedValue = converter.toUnicode(s, conversionWarnings, true);
	    System.out.println("converting \""+s+"\" into "+convertedValue);
	    if (conversionWarnings.size() > 0) {
	        System.out.println("with warnings: "+String.join(", ", conversionWarnings));
	    }
	    return convertedValue;
	}
	
	@Test
	public void textEwts() {
	    List<String> conversionWarnings = new ArrayList<String>();
	    String res = toUnicode("pa'ng", conversionWarnings);
	    assertTrue(res.equals("པའང"));
	    assertTrue(conversionWarnings.size()==0);
	    conversionWarnings = new ArrayList<String>();
	    res = toUnicode("be'u'i'o", conversionWarnings);
        assertTrue(res.equals("བེའུའིའོ"));
        assertTrue(conversionWarnings.size()==0);
        conversionWarnings = new ArrayList<String>();
        res = toUnicode("pa'm", conversionWarnings);
        assertTrue(res.equals("པའམ"));
        assertTrue(conversionWarnings.size()==0);
        assertTrue(CommonMigration.normalizeTibetan("དྷ་དཹ་").equals("དྷ་དླཱྀ་"));
        assertTrue(CommonMigration.normalizeTibetan("\u0F81").equals("\u0F71\u0F80"));
        assertTrue(CommonMigration.normalizeTibetan("\u0F76").equals("\u0FB2\u0F80"));
	}
	
	@Test
    public void testP1583()
    {
	    System.out.println("testing P1531");
	    Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1583.xml");
    	assertTrue(CommonMigration.documentValidates(d, personValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1583.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testG844()
    {
	    System.out.println("testing G844");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/G844.xml");
    	assertFalse(CommonMigration.documentValidates(d, placeValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "place");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/G844.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "place", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testPR99NCUL01()
    {
	    System.out.println("testing product");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PR99NCUL01.xml");
    	assertTrue(CommonMigration.documentValidates(d, productValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "product");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/PR99NCUL01.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "product", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testCorporation()
    {
	    System.out.println("testing corporation");
		Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/CorporationTest.xml");	
		assertTrue(CommonMigration.documentValidates(d, corporationValidator));
		Model fromXml = MigrationHelpers.xmlToRdf(d, "corporation");
		Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/CorporationTest.jsonld");
		//MigrationHelpers.modelToOutputStream(fromXml, System.out, "corporation", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	   @Test
	    public void testWork()
	    {
	       System.out.println("testing work");
	        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/WorkTest.xml");  
	        assertFalse(CommonMigration.documentValidates(d, workValidator));
	        Model fromXml = MigrationHelpers.xmlToRdf(d, "work");
	        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/WorkTest.jsonld");
	        //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", true);
	        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
	        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
	        flushLog();
	    }
	   
       @Test
       public void testOutline()
       {
           System.out.println("testing outline");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/OutlineTest.xml");  
           assertTrue(CommonMigration.documentValidates(d, outlineValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "outline");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/OutlineTest.jsonld");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "outline", true);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testPubinfo()
       {
           System.out.println("testing pubinfo");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PubinfoTest.xml");  
           //assertTrue(CommonMigration.documentValidates(d, pubinfoValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "pubinfo");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/PubinfoTest.jsonld");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", true);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
	   
       @Test
       public void testOffice()
       {
           System.out.println("testing office");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/OfficeTest.xml");  
           assertTrue(CommonMigration.documentValidates(d, officeValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "office");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/OfficeTest.jsonld");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "office", true);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testTopic()
       {
           System.out.println("testing topic");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/TopicTest.xml");  
           assertTrue(CommonMigration.documentValidates(d, topicValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "topic");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/TopicTest.jsonld");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "topic", true);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testScanrequest()
       {
           System.out.println("testing scanrequest");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/ScanrequestTest.xml");  
           assertTrue(CommonMigration.documentValidates(d, scanrequestValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "scanrequest");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/ScanrequestTest.jsonld");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "volumes", true);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testImagegroup()
       {
           System.out.println("testing imagegroup");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/ImagegroupTest.xml");  
           assertTrue(CommonMigration.documentValidates(d, imagegroupValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "imagegroup");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/ImagegroupTest.jsonld");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "volumes", true);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }

	@Test
    public void testL8LS14115()
    {
	    System.out.println("testing lineage");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/L8LS14115.xml");
    	assertTrue(CommonMigration.documentValidates(d, lineageValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "lineage");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/L8LS14115.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "lineage", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
}
