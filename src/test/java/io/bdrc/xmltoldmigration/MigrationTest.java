package io.bdrc.xmltoldmigration;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

import io.bdrc.xmltoldmigration.MigrationHelpers;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertTrue;

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
	
	@Before
	public void init() {
		ontology = MigrationHelpers.getOntologyModel();
		personValidator = MigrationHelpers.getValidatorFor("person");
		placeValidator = MigrationHelpers.getValidatorFor("place");
		lineageValidator = MigrationHelpers.getValidatorFor("lineage");
		productValidator = MigrationHelpers.getValidatorFor("product");
		corporationValidator = MigrationHelpers.getValidatorFor("corporation");
		workValidator = MigrationHelpers.getValidatorFor("work");
	}
	
	@Test
    public void testP1331()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
		assertTrue(CommonMigration.documentValidates(d, personValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1331.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        
    }
	
	@Test
    public void testP1583()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1583.xml");
    	assertTrue(CommonMigration.documentValidates(d, personValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1583.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "person", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
    }
	
	@Test
    public void testG844()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/G844.xml");
    	assertTrue(CommonMigration.documentValidates(d, placeValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "place");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/G844.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "place", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
    }
	
	@Test
    public void testPR99NCUL01()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/PR99NCUL01.xml");
    	assertTrue(CommonMigration.documentValidates(d, productValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "product");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/PR99NCUL01.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "product", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
    }
	
	@Test
    public void testCorporation()
    {
		Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/CorporationTest.xml");	
		assertTrue(CommonMigration.documentValidates(d, corporationValidator));
		Model fromXml = MigrationHelpers.xmlToRdf(d, "corporation");
		Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/CorporationTest.jsonld");
		//MigrationHelpers.modelToOutputStream(fromXml, System.out, "corporation", true);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
    }
	
	   @Test
	    public void testWork()
	    {
	        Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/WorkTest.xml");  
	        assertTrue(CommonMigration.documentValidates(d, workValidator));
	        Model fromXml = MigrationHelpers.xmlToRdf(d, "work");
	        //Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/WorkTest.jsonld");
	        MigrationHelpers.modelToOutputStream(fromXml, System.out, "work", true);
	        //assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
	        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
	    }

	@Test
    public void testL8LS14115()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/L8LS14115.xml");
    	assertTrue(CommonMigration.documentValidates(d, lineageValidator));
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "lineage");
    	//Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1583.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out, "lineage", true);
        //assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
    }
}
