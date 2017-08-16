package io.bdrc.xmltoldmigration;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;
import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

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
	final static String TESTDIR = "src/test/";
	public static OntModel ontology = null;
	public static final EwtsConverter converter = new EwtsConverter();
	
	@BeforeClass
	public static void init() {
	    MigrationHelpers.usecouchdb = false;
		ontology = MigrationHelpers.ontologymodel;
		SymetricNormalization.normalizeOneDirection(true, false);
	}
	
   @AfterClass
    public static void close() {
        CommonMigration.speller.close();
        ExceptionHelper.closeAll();
        System.out.println("finishing with the following triples to be added:");
        System.out.println(SymetricNormalization.triplesToAdd.toString());
    }

	public void flushLog() {
	    try {
            MigrationHelpers.logWriter.flush();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	public void showDifference (Model src, Model dst) {
	    Model plus = dst.difference(src);
	    Model minus = src.difference(dst);
	    System.out.println("plus:");
	    System.out.println(plus.toString());
	    System.out.println("minus:");
        System.out.println(minus.toString());
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
	public void testUrlNormalization() {
	    assertTrue(CommonMigration.normalizeToLUrl("http://treasuryoflives.org/biographies/abc").equals("https://www.treasuryoflives.org/biographies/abc"));
	    assertTrue(CommonMigration.normalizeToLUrl("http://beta.treasuryoflives.org/biographies/abc").equals("https://www.treasuryoflives.org/biographies/abc"));
	    assertTrue(CommonMigration.getRIDFromTbrcUrl("http://tbrc.org/#library_work_Object-W00EGS1016761").equals("W00EGS1016761"));
	    assertTrue(CommonMigration.getRIDFromTbrcUrl("http://tbrc.org/link?RID=O2DB102429|O2DB1024292DB102470$W21634").equals("O2DB1024292DB102470"));
	    assertTrue(CommonMigration.getRIDFromTbrcUrl("http://www.tbrc.org/link/?RID=O1KG4084|O1KG40841KG4095$W1KG3381#library_work_Object-O1KG4084|O1KG40841KG4095$W1KG3381").equals("O1KG40841KG4095"));
	    assertTrue(CommonMigration.getRIDFromTbrcUrl("http://mercury.tbrc.org/link?RID=O3LS12537|O3LS125373LS13489$W8039").equals("O3LS125373LS13489"));
	    assertTrue(CommonMigration.getRIDFromTbrcUrl("http://tbrc.org/?locale=bo#library_work_Object-W1PD107999").equals("W1PD107999"));
	    assertTrue(CommonMigration.getRIDFromTbrcUrl("http://tbrc.org/link/?RID=T1CZ28#library_topic_Object-T1CZ28").equals("T1CZ28"));
	}
	
	@Test
	public void testHunspell() {
	    assertTrue(CommonMigration.isStandardTibetan("བོད"));
	    assertTrue(CommonMigration.isStandardTibetan("བོད་བོད་ བོད་"));
	    assertFalse(CommonMigration.isStandardTibetan("བབབོ་ད་དདཨོ་"));
	    assertFalse(CommonMigration.isStandardTibetan("བབབོ་ད་དདཨོ་"));
	    assertFalse(CommonMigration.isStandardTibetan("བོད a"));
	    assertFalse(CommonMigration.isStandardTibetan("abc"));
	    assertFalse(CommonMigration.isStandardTibetan("རཀག"));
	}
	
	@Test
	public void testNormalize() {
	    assertTrue(CommonMigration.normalizeString("").equals(""));
	    String allWhiteSpaces = " 1 \u0009 2 \n 3 \u000C 4 \r 5 \u0020 6 \u0085 7 \u00A0 8 \u1680 9 \u180E 10 \u2000 11 \u2001 12 \u2002 13 \u2003 14 \u2004 15 \u2005 16 \u2006 17 \u2007 18 \u2008 19 \u2009 20 \u200A 21 \u2028 22 \u2029 23 \u202F 24 \u205F 25 \u3000 26 \0 27 ";
	    assertTrue(CommonMigration.normalizeString(allWhiteSpaces).equals("1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27"));
	}
	
   @Test
    public void testP1331()
    {
        System.out.println("testing P1331");
        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
        Validator validator = MigrationHelpers.getValidatorFor("person");
        assertTrue(CommonMigration.documentValidates(d, validator));
        Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/P1331.ttl");
        //MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", MigrationHelpers.OUTPUT_STTL);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testP1583()
    {
	    System.out.println("testing P1583");
	    Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1583.xml");
	    Validator validator = MigrationHelpers.getValidatorFor("person");
        assertTrue(CommonMigration.documentValidates(d, validator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/P1583.ttl");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", MigrationHelpers.OUTPUT_STTL);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testG844() throws JsonGenerationException, JsonLdError, IOException
    {
	    System.out.println("testing G844");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/G844.xml");
    	Validator validator = MigrationHelpers.getValidatorFor("place");
        assertFalse(CommonMigration.documentValidates(d, validator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "place");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/G844.ttl");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "place", MigrationHelpers.OUTPUT_STTL);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testPR99NCUL01()
    {
	    System.out.println("testing product");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PR99NCUL01.xml");
    	Validator validator = MigrationHelpers.getValidatorFor("product");
        assertTrue(CommonMigration.documentValidates(d, validator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "product");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/PR99NCUL01.ttl");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "product", MigrationHelpers.OUTPUT_STTL);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testCorporation()
    {
	    System.out.println("testing corporation");
		Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/CorporationTest.xml");	
		Validator validator = MigrationHelpers.getValidatorFor("corporation");
        assertTrue(CommonMigration.documentValidates(d, validator));
		Model fromXml = MigrationHelpers.xmlToRdf(d, "corporation");
		Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/CorporationTest.ttl");
		//MigrationHelpers.modelToOutputStream(fromXml, System.out, "corporation", MigrationHelpers.OUTPUT_STTL);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	   @Test
	    public void testWork() throws JsonLdError, JsonParseException, IOException
	    {
	        System.out.println("testing work");
	        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/WorkTest.xml");  
	        Validator validator = MigrationHelpers.getValidatorFor("work");
	        assertFalse(CommonMigration.documentValidates(d, validator));
	        Model fromXml = MigrationHelpers.xmlToRdf(d, "work");
	        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/WorkTest.ttl");
	        //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", MigrationHelpers.OUTPUT_STTL);
	        //showDifference(fromXml, correctModel);
	        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
	        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
	        flushLog();
	    }
	   
       @Test
       public void testOutline() throws JsonParseException, IOException, JsonLdError
       {
           System.out.println("testing outline");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/OutlineTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("outline");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "outline");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/OutlineTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "outline", MigrationHelpers.OUTPUT_STTL);
           //showDifference(fromXml, correctModel);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testPubinfo()
       {
           System.out.println("testing pubinfo");
           WorkMigration.splitItems = false;
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PubinfoTest.xml");  
           //assertTrue(CommonMigration.documentValidates(d, pubinfoValidator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "pubinfo");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/PubinfoTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", MigrationHelpers.OUTPUT_STTL);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
	   
       @Test
       public void testOffice()
       {
           System.out.println("testing office");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/OfficeTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("office");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "office");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/OfficeTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "office", MigrationHelpers.OUTPUT_STTL);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
//       
       @Test
       public void testTopic()
       {
           System.out.println("testing topic");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/TopicTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("topic");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "topic");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/TopicTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "topic", MigrationHelpers.OUTPUT_STTL);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testScanrequest()
       {
           System.out.println("testing scanrequest");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/ScanrequestTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("scanrequest");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "scanrequest");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/ScanrequestTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "item", MigrationHelpers.OUTPUT_STTL);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testImagegroup()
       {
           System.out.println("testing imagegroup");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/ImagegroupTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("imagegroup");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "imagegroup");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/ImagegroupTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "item", MigrationHelpers.OUTPUT_STTL);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }

	@Test
    public void testL8LS14115()
    {
	    System.out.println("testing lineage");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/L8LS14115.xml");
    	Validator validator = MigrationHelpers.getValidatorFor("lineage");
        assertTrue(CommonMigration.documentValidates(d, validator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "lineage");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/L8LS14115.ttl");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "lineage", MigrationHelpers.OUTPUT_STTL);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
}
