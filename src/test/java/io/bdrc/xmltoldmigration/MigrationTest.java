package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.xmltoldmigration.MigrationHelpers.OUTPUT_STTL;
import static io.bdrc.xmltoldmigration.MigrationHelpers.OUTPUT_TRIG;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.setPrefixes;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Validator;
import javax.xml.xpath.XPathExpressionException;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.github.jsonldjava.core.JsonLdError;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.ImageListTranslation;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextBodyMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextMigration.EtextInfos;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;
import io.bdrc.xmltoldmigration.xml2files.PersonMigration;
import io.bdrc.xmltoldmigration.xml2files.PubinfoMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.WorkModelInfo;


/**
 * Unit test for simple App.
 */
public class MigrationTest 
{
	final static String TESTDIR = "src/test/";
	public static final EwtsConverter converter = new EwtsConverter();
	
	@BeforeClass
	public static void init() throws NoSuchAlgorithmException {
		SymetricNormalization.normalizeOneDirection(true, false);
		WorkMigration.splitItems = false;
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
	public void testGetLiteral() {
        try {
            System.out.println("testing getLiteral");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element elem = doc.createElementNS("http://www.tbrc.org/models/work#", "w:title");

            elem.setTextContent("maṅgalatthadīpanī aṭṭhakathāmaṅgalasūtra");
            elem.setAttribute("lang", "pāli");
            elem.setAttribute("encoding", "kmfemc");
            Model model = ModelFactory.createDefaultModel();
            String dflt = EWTS_TAG;
            String propHint = "title";
            String RID = "W1FEMC010006";
            Literal lit = CommonMigration.getLiteral(elem, dflt, model, propHint, RID, RID);
            assertTrue("pi-x-kmfemc".equals(lit.getLanguage()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
    
    @Test
    public void testW1FEMC010006() throws IOException {
        System.out.println("testing W1FEMC010006");
        Document doc = MigrationHelpers.documentFromFileName(TESTDIR+"xml/W1FEMC010006.xml");
        Model fromXml = mergeModelInfoList(WorkMigration.MigrateWork(doc));
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/W1FEMC010006.ttl");
//      
//      // ==== TEMP DEBUG ====
//        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testW1FEMC010006-fromXml.ttl"), "TTL");
//        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testW1FEMC010006-correctModel.ttl"), "TTL");
//
        //fromXml.write(System.out, "TTL");
        assertTrue( fromXml.isIsomorphicWith(correctModel) );
    }

    @Test
    public void testW1FEMC020013() throws IOException {
        System.out.println("testing W1FEMC020013");
        Document doc = MigrationHelpers.documentFromFileName(TESTDIR+"xml/W1FEMC020013.xml");
        Model fromXml = mergeModelInfoList(WorkMigration.MigrateWork(doc));
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/W1FEMC020013.ttl");
//      
//      // ==== TEMP DEBUG ====
//        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testW1FEMC020013-fromXml.ttl"), "TTL");
//        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testW1FEMC020013-correctModel.ttl"), "TTL");
//
        //fromXml.write(System.out, "TTL");
        assertTrue( fromXml.isIsomorphicWith(correctModel) );
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
        //fromXml.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
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
    	//fromXml.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        flushLog();
    }
    
    @Test
    public void testG488() throws JsonGenerationException, JsonLdError, IOException
    {
        System.out.println("testing G488");
        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/G488.xml");
        Validator validator = MigrationHelpers.getValidatorFor("place");
        assertFalse(CommonMigration.documentValidates(d, validator));
        Model fromXml = MigrationHelpers.xmlToRdf(d, "place");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/G488.ttl");
        //fromXml.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
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
        //fromXml.write(System.out, "TTL");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/G844.ttl");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
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
    	//fromXml.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
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
		fromXml.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        flushLog();
    }
	
   @Test
    public void testWork() throws JsonLdError, JsonParseException, IOException
    {
        System.out.println("testing work");
        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/WorkTestFPL.xml");  
        Validator validator = MigrationHelpers.getValidatorFor("work");
        assertFalse(CommonMigration.documentValidates(d, validator));
        Model fromXml = mergeModelInfoList(WorkMigration.MigrateWork(d));
        //fromXml.write(System.out, "TTL");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/WorkTestFPL.ttl");
        //MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", MigrationHelpers.OUTPUT_STTL, "");
        //showDifference(fromXml, correctModel);
        //fromXml.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        flushLog();
    }
	   
       public static Model mergeModelList(List<Model> list) {
           Model res = ModelFactory.createDefaultModel();
           setPrefixes(res);
           for (Model m : list) {
               if (m != null)
                   res.add(m);
           }
           return res;
       }

       public static Model mergeResources(List<Resource> list) {
           Model res = ModelFactory.createDefaultModel();
           setPrefixes(res);
           for (Resource r : list) {
               if (r != null)
                   res.add(r.getModel());
           }
           return res;
       }
       
       public static Model mergeModelInfoList(List<WorkModelInfo> list) {
           Model res = ModelFactory.createDefaultModel();
           setPrefixes(res);
           for (WorkModelInfo mi : list) {
               if (mi != null && mi.m != null)
                   res.add(mi.m);
           }
           return res;
       }
       
       @Test
       public void testOutline() throws JsonParseException, IOException, JsonLdError
       {
           System.out.println("testing outline");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/OutlineTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("outline");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = mergeModelInfoList(OutlineMigration.MigrateOutline(d));
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/OutlineTest.ttl");
           //showDifference(fromXml, correctModel);
//           fromXml.write(System.out, "TURTLE");
//         
//         // ==== TEMP DEBUG ====
//           fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testOutline-fromXml.ttl"), "TTL");
//           correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testOutline-correctModel.ttl"), "TTL");
//
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }
       
       @Test
       public void testPubinfo() throws JsonLdError, JsonParseException, IOException
       {
           System.out.println("testing pubinfo");
           WorkMigration.splitItems = false;
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PubinfoTest.xml");  
           //assertTrue(CommonMigration.documentValidates(d, pubinfoValidator));
           Model fromXml = mergeModelList(PubinfoMigration.MigratePubinfo(d));
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/PubinfoTest.ttl");
           //fromXml.write(System.out, "TTL");
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }
	   
       @Test
       public void testOffice() throws IOException
       {
           System.out.println("testing office");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/OfficeTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("office");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "office");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/OfficeTest.ttl");
           //fromXml.write(System.out, "TTL");
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }
       
       @Test
       public void testTopic() throws IOException
       {
           System.out.println("testing topic");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/TopicTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("topic");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "topic");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/TopicTest.ttl");
           //fromXml.write(System.out, "TTL");
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }

       @Test
       public void testTaxonomy() throws IOException
       {
           System.out.println("testing taxonomy");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/TaxonomyTest.xml");
           Validator validator = MigrationHelpers.getValidatorFor("outline");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "taxonomy");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/TaxonomyTest.ttl");
           //fromXml.write(System.out, "TTL");
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }
       
       @Test
       public void testScanrequest() throws IOException
       {
           System.out.println("testing scanrequest");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/ScanrequestTest.xml");  
           Validator validator = MigrationHelpers.getValidatorFor("scanrequest");
           assertTrue(CommonMigration.documentValidates(d, validator));
           Model fromXml = MigrationHelpers.xmlToRdf(d, "scanrequest");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/ScanrequestTest.ttl");
           //MigrationHelpers.modelToOutputStream(fromXml, System.out, "item", MigrationHelpers.OUTPUT_STTL);
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }

       @Test
       public void testWithdrawn() throws IOException
       {
           System.out.println("testing withdrawn record");
           Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/withdrawn-test.xml");  
           Model fromXml = MigrationHelpers.migrateWithdrawn(d, "office");
           //fromXml.write(System.out, "TTL");
           Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/withdrawn-test.ttl");
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
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
           fromXml.write(System.out, "TTL");
           assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
           flushLog();
       }

	@Test
    public void testL8LS14115() throws IOException
    {
	    System.out.println("testing lineage");
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/L8LS14115.xml");
    	Validator validator = MigrationHelpers.getValidatorFor("lineage");
        assertTrue(CommonMigration.documentValidates(d, validator));
        Model fromXml = MigrationHelpers.xmlToRdf(d, "lineage");
        Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/L8LS14115.ttl");
        //fromXml.write(System.out, "TTL");
//      
//      // ==== TEMP DEBUG ====
//        fromXml.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testL8LS14115-fromXml.ttl"), "TTL");
//        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/testL8LS14115-correctModel.ttl"), "TTL");
//
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
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
    public void testEtext() throws XPathExpressionException, IOException
    {
        System.out.println("testing etext");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Model itemModel = ModelFactory.createDefaultModel();
        MigrationHelpers.setPrefixes(itemModel, "item");
        EtextInfos ei = EtextMigration.migrateOneEtext(TESTDIR+"xml/EtextTest.xml", true, out, false, itemModel, true, BDA+"CP001");
        String computedContent = new String( out.toByteArray(), StandardCharsets.UTF_8 );
        assertTrue(ei.eInstanceId.equals("IE1CZ2485"));
        assertTrue(ei.indicatedWorkId.equals("W1CZ2485"));
        assertTrue(ei.etextId.equals("UT1CZ2485_001_0000"));
        //MigrationHelpers.modelToOutputStream(ei.etextModel, System.out, "etext", MigrationHelpers.OUTPUT_STTL, ei.etextId);
        //MigrationHelpers.modelToOutputStream(itemModel, System.out, "item", MigrationHelpers.OUTPUT_STTL, ei.itemId);
        //System.out.println(computedContent);
        Model correctEtextModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/EtextTest-etext.ttl");
        Model correctItemModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/EtextTest-item.ttl");
        String correctContent = new String(Files.readAllBytes(Paths.get(TESTDIR+"ttl/EtextTest-content.txt")));
        //ei.etextModel.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(ei.etextModel, correctEtextModel) );
        //itemModel.write(System.out, "TTL");
        assertTrue( MigrationHelpers.isSimilarTo(itemModel, correctItemModel) );
        assertTrue(computedContent.equals(correctContent.trim()));
        assertFalse(EtextBodyMigration.rtfP.matcher(" 9 ").find());
        assertTrue(EtextBodyMigration.rtfP.matcher("1$0000270").find());
        assertTrue(EtextBodyMigration.rtfP.matcher("PAGE -PAGE 2--PAGE 1-").find());
        assertTrue(EtextBodyMigration.rtfP.matcher("PAGE \\* MERGEFORMAT 2").find());
        // test with different options:
        out = new ByteArrayOutputStream();
        itemModel = ModelFactory.createDefaultModel();
        MigrationHelpers.setPrefixes(itemModel, "item");
        ei = EtextMigration.migrateOneEtext(TESTDIR+"xml/EtextTest.xml", false, out, false, itemModel, true, BDA+"CP001");
        computedContent = new String( out.toByteArray(), StandardCharsets.UTF_8 );
        //System.out.println(computedContent);
        // this one is a bit bogus because it adds spaces in line milestones, but in real life data there is no lines when we must
        // no keep the pagination
        correctContent = new String(Files.readAllBytes(Paths.get(TESTDIR+"ttl/EtextTest-content-noPages.txt")));
        assertTrue(computedContent.equals(correctContent.trim()));
        flushLog();
    }
    
    @Test
    public void testEtextReadItem() throws XPathExpressionException, IOException
    {
        Model itemModel = MigrationHelpers.modelFromFileName(TESTDIR+"ttl/ImagegroupTest.ttl");
        int foundVol = EtextMigration.getVolumeNumber("4158", itemModel, null);
        assertTrue(foundVol == 1);
    }
    
}
