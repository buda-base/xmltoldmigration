package io.bdrc.xmltoldmigration;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

import io.bdrc.xmltoldmigration.MigrationHelpers;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertTrue;


/**
 * Unit test for simple App.
 */
public class MigrationTest 
{
	final String TESTDIR = "src/test/";
	OntModel ontology = null;
	
	@Before
	public void init() {
		ontology = MigrationHelpers.getOntologyModel();
	}
	
	@Test
    public void testP1331()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1331.jsonld");
    	//MigrationHelpers.modelToOutputStream(fromXml, System.out);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        
    }
	
	@Test
    public void testP1583()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1583.xml");
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1583.jsonld");
    	MigrationHelpers.modelToOutputStream(fromXml, System.out);
        assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        assertTrue( CommonMigration.rdfOkInOntology(fromXml, ontology) );
        
    }
}
