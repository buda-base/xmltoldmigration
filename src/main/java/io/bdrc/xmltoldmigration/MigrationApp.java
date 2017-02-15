package io.bdrc.xmltoldmigration;

/**
 * Hello world!
 *
 */
public class MigrationApp 
{
    public static void main( String[] args )
    {
    	final String TESTDIR = "src/test/xml/";
    	MigrationHelpers.convertOneFile(TESTDIR+"P1331.xml", TESTDIR+"P1331.jsonld", "Person");
    }
}
