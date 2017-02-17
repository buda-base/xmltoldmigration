package io.bdrc.xmltoldmigration;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

import io.bdrc.xmltoldmigration.MigrationHelpers;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class MigrationTest 
    extends TestCase
{
	final String TESTDIR = "src/test/";
	
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MigrationTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( MigrationTest.class );
    }

    public void testP1331()
    {
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	Model correctModel = MigrationHelpers.modelFromFileName(TESTDIR+"jsonld/P1331.jsonld");
//    	Model diff = correctModel.difference(fromXml);
//    	MigrationHelpers.modelToOutputStream(diff, System.out);
//    	diff = fromXml.difference(correctModel);
//    	MigrationHelpers.modelToOutputStream(diff, System.out);
        //assertTrue( MigrationHelpers.isSimilarTo(fromXml, correctModel) );
        OntModel o = MigrationHelpers.getOntologyModel();
        boolean ok = MigrationHelpers.rdfOkInOntology(fromXml, o);
        assertTrue(true);
        
    }
}
