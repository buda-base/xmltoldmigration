package io.bdrc.xmltoldmigration;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class MigrationTest 
    extends TestCase
{
	final String TESTDIR = "src/test/xml/";
	
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
        assertTrue( true );
    }
}
