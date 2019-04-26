package io.bdrc.xmltoldmigration;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;
import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.ImageListTranslation;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextBodyMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextMigration.EtextInfos;
import io.bdrc.xmltoldmigration.xml2files.PersonMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;


/**
 * Unit test for simple App.
 */
public class MigrationTest 
{
	final static String TESTDIR = "src/test/";
	public static OntModel ontology = null;
	public static final EwtsConverter converter = new EwtsConverter();
	
	@BeforeClass
	public static void init() throws NoSuchAlgorithmException {
		ontology = MigrationHelpers.ontologymodel;
		SymetricNormalization.normalizeOneDirection(true, false);
		WorkMigration.splitItems = false;
		MigrationApp.md = MessageDigest.getInstance("MD5");
		EtextMigration.testMode = true;
	}
	
   @AfterClass
    public static void close() {
        CommonMigration.speller.close();
        ExceptionHelper.closeAll();
        System.out.println("finishing with the following triples to be added:");
        System.out.println(SymetricNormalization.triplesToAdd.toString());
        System.out.println("and the following person events to be added in places:");
        System.out.println(PersonMigration.placeEvents.toString());
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
	public void testEwtsShad() {
	    assertTrue("bla ma/".equals(CommonMigration.addEwtsShad("bla ma")));
	    assertTrue("ngo /".equals(CommonMigration.addEwtsShad("ngo")));
	    assertTrue("nga /".equals(CommonMigration.addEwtsShad("nga")));
	    assertTrue("ngag".equals(CommonMigration.addEwtsShad("ngag")));
	    assertTrue("ga".equals(CommonMigration.addEwtsShad("ga")));
	    assertTrue("gi".equals(CommonMigration.addEwtsShad("gi")));
	    assertTrue("she".equals(CommonMigration.addEwtsShad("she")));
	    assertTrue("tshe/".equals(CommonMigration.addEwtsShad("tshe")));
	    assertTrue("ko".equals(CommonMigration.addEwtsShad("ko")));
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
        assertTrue(CommonMigration.isMostLikelyEwts("myang stod khu le'i rgya rigs"));
        assertFalse(CommonMigration.isMostLikelyEwts("my tailor is rich"));
        assertFalse(CommonMigration.isMostLikelyEwts("associated w / 11th cent. master, P4CZ15480 ?"));
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
    public void testP1331() throws IOException
    {
        System.out.println("testing P1331");
        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
        Validator validator = MigrationHelpers.getValidatorFor("person");
        assertTrue(CommonMigration.documentValidates(d, validator));
        Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/P1331.ttl");
        //MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", MigrationHelpers.OUTPUT_STTL);
        
        // ==== TEMP DEBUG ====
        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testP1331-fromXml.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testP1331-correctModel.ttl"), "TTL");

        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testP1583() throws IOException
    {
	    System.out.println("testing P1583");
	    Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1583.xml");
	    Validator validator = MigrationHelpers.getValidatorFor("person");
        assertTrue(CommonMigration.documentValidates(d, validator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/P1583.ttl");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", MigrationHelpers.OUTPUT_STTL, "");
        
        // ==== TEMP DEBUG ====
        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testP1583-fromXml.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testP1583-correctModel.ttl"), "TTL");

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
        
    	// ==== TEMP DEBUG ====
    	fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-G844-fromXml.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-G844-correctModel.ttl"), "TTL");

        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testPR99NCUL01() throws IOException
    {
	    System.out.println("testing product");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PR99NCUL01.xml");
    	Validator validator = MigrationHelpers.getValidatorFor("product");
        assertTrue(CommonMigration.documentValidates(d, validator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "product");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/PR99NCUL01.ttl");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "product", MigrationHelpers.OUTPUT_STTL, null);
        
        // ==== TEMP DEBUG ====
        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testPR99NCUL01-fromXml.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testPR99NCUL01-correctModel.ttl"), "TTL");

        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
    public void testCorporation() throws IOException
    {
	    System.out.println("testing corporation");
		Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/CorporationTest.xml");	
		Validator validator = MigrationHelpers.getValidatorFor("corporation");
        assertTrue(CommonMigration.documentValidates(d, validator));
		Model fromXml = MigrationHelpers.xmlToRdf(d, "corporation");
		Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/CorporationTest.ttl");
		MigrationHelpers.modelToOutputStream(fromXml, System.out, "corporation", MigrationHelpers.OUTPUT_TRIG, null);
        
        // ==== TEMP DEBUG ====
        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testCorporation-fromXml.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testCorporation-correctModel.ttl"), "TTL");

        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
   @Test
    public void testWork() throws JsonLdError, JsonParseException, IOException
    {
        System.out.println("testing work");
        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/WorkTestFPL.xml");  
        Validator validator = MigrationHelpers.getValidatorFor("work");
        assertFalse(CommonMigration.documentValidates(d, validator));
        Model fromXml = MigrationHelpers.xmlToRdf(d, "work");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/WorkTestFPL.ttl");
        //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", MigrationHelpers.OUTPUT_STTL, "");
        //showDifference(fromXml, correctModel);
        
        // ==== TEMP DEBUG ====
        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testWork-fromXml.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testWork-correctModel.ttl"), "TTL");

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
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", MigrationHelpers.OUTPUT_STTL, "W30020");
           //showDifference(fromXml, correctModel);
           
           // ==== TEMP DEBUG ====
           fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testOutline-fromXml.ttl"), "TTL");
           correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testOutline-correctModel.ttl"), "TTL");

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
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", MigrationHelpers.OUTPUT_STTL, "");
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
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "topic", MigrationHelpers.OUTPUT_STTL, null);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }

       @Test
       public void testTaxonomy()
       {
           System.out.println("testing taxonomy");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/TaxonomyTest.xml");
           Validator validator = MigrationHelpers.getValidatorFor("outline");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "taxonomy");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/TaxonomyTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "topic", MigrationHelpers.OUTPUT_STTL, null);
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
       public void testWithdrawn() throws IOException
       {
           System.out.println("testing withdrawn record");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/withdrawn-test.xml");  
           Model fromXml = MigrationHelpers.migrateWithdrawn(d, "office");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/withdrawn-test.ttl");
           
           // ==== TEMP DEBUG ====
           fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testWithdrawn-fromXml.ttl"), "TTL");
           correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testWithdrawn-correctModel.ttl"), "TTL");

           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
           flushLog();
       }
       
       @Test
       public void testImagegroup() throws IOException
       {
           System.out.println("testing imagegroup");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/ImagegroupTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("imagegroup");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "imagegroup");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/ImagegroupTest.ttl");
           //System.out.println(ImageListTranslation.getImageNums(correctModel, 1));
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "item", MigrationHelpers.OUTPUT_STTL, "");
           
           // ==== TEMP DEBUG ====
           fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testImagegroup-fromXml.ttl"), "TTL");
           correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/MIGRATION_TEST-testImagegroup-correctModel.ttl"), "TTL");

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
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "lineage", MigrationHelpers.OUTPUT_STTL, "");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        flushLog();
    }
	
	@Test
	public void testImageList() {
	    System.out.println("testing image list");
	    Map<String,Integer> imageNums = ImageListTranslation.getImageNums("49050001.tif:3", null);
	    Map<String,Integer> expected = new HashMap<>();
	    expected.put("49050001.tif", 1);
	    expected.put("49050002.tif", 2);
	    expected.put("49050003.tif", 3);
        assertEquals(expected, imageNums);
        imageNums = ImageListTranslation.getImageNums("49050025.tif:3", null);
        expected = new HashMap<>();
        expected.put("49050025.tif", 1);
        expected.put("49050026.tif", 2);
        expected.put("49050027.tif", 3);
        assertEquals(expected, imageNums);
        imageNums = ImageListTranslation.getImageNums("49050025.tif:2|49050028.tif:2", "1-24,27");
        expected = new HashMap<>();
        expected.put("49050025.tif", 25);
        expected.put("49050026.tif", 26);
        expected.put("49050028.tif", 28);
        expected.put("49050029.tif", 29);
        assertEquals(expected, imageNums);
	}
	
	@Test
	public void testEtextIndexTranslation() {
	    List<Integer> breaks = Arrays.asList(2);
	    assertTrue(Arrays.equals(EtextBodyMigration.translatePoint(breaks, 2, true), new int[] {1,2}));
        assertTrue(Arrays.equals(EtextBodyMigration.translatePoint(breaks, 2, false), new int[] {1,2}));
        assertTrue(Arrays.equals(EtextBodyMigration.translatePoint(breaks, 3, true), new int[] {2,1}));
        assertTrue(Arrays.equals(EtextBodyMigration.translatePoint(breaks, 3, false), new int[] {2,1}));
        assertTrue(Arrays.equals(EtextBodyMigration.translatePoint(breaks, 4, false), new int[] {2,2}));
	}
	
    @Test
    public void testEtext() throws XPathExpressionException, IOException
    {
        System.out.println("testing etext");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Model itemModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(itemModel, "item");
        EtextInfos ei = EtextMigration.migrateOneEtext(TESTDIR+"xml/EtextTest.xml", true, out, false, itemModel, true);
        String computedContent = new String( out.toByteArray(), StandardCharsets.UTF_8 );
        assertTrue(ei.itemId.equals("I1CZ2485_E001"));
        assertTrue(ei.workId.equals("W1CZ2485"));
        assertTrue(ei.etextId.equals("UT1CZ2485_001_0000"));
        //MigrationHelpers.modelToOutputStream(ei.etextModel, System.out, "etext", MigrationHelpers.OUTPUT_STTL, ei.etextId);
        //MigrationHelpers.modelToOutputStream(itemModel, System.out, "item", MigrationHelpers.OUTPUT_STTL, ei.itemId);
        //System.out.println(computedContent);
        Model correctEtextModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/EtextTest-etext.ttl");
        Model correctItemModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/EtextTest-item.ttl");
        String correctContent = new String(Files.readAllBytes(Paths.get(TESTDIR+"ttl/EtextTest-content.txt")));
        assertTrue( MigrationHelpers.isSimilarTo(ei.etextModel, correctEtextModel) );
        assertTrue( MigrationHelpers.isSimilarTo(itemModel, correctItemModel) );
        assertTrue(computedContent.equals(correctContent.trim()));
        assertFalse(EtextBodyMigration.rtfP.matcher(" 9 ").find());
        assertTrue(EtextBodyMigration.rtfP.matcher("1$0000270").find());
        assertTrue(EtextBodyMigration.rtfP.matcher("PAGE -PAGE 2--PAGE 1-").find());
        assertTrue(EtextBodyMigration.rtfP.matcher("PAGE \\* MERGEFORMAT 2").find());
        // test with different options:
        out = new ByteArrayOutputStream();
        itemModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(itemModel, "item");
        ei = EtextMigration.migrateOneEtext(TESTDIR+"xml/EtextTest.xml", false, out, false, itemModel, true);
        computedContent = new String( out.toByteArray(), StandardCharsets.UTF_8 );
        //System.out.println(computedContent);
        // this one is a bit bogus because it adds spaces in line milestones, but in real life data there is no lines when we must
        // no keep the pagination
        correctContent = new String(Files.readAllBytes(Paths.get(TESTDIR+"ttl/EtextTest-content-noPages.txt")));
        assertTrue(computedContent.equals(correctContent.trim()));
        flushLog();
    }
}
