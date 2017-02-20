package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

/**
 * Hello world!
 *
 */
public class MigrationApp 
{
    public static void main( String[] args )
    {
    	final String TESTDIR = "src/test/";
    	Document d = MigrationHelpers.documentFromFileName(TESTDIR+"xml/P1331.xml");
    	Model fromXml = MigrationHelpers.xmlToRdf(d, "person");
    	MigrationHelpers.modelToOutputStream (fromXml, System.out);
    }
}
