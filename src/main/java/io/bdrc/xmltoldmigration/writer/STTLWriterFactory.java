package io.bdrc.xmltoldmigration.writer;

import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterGraphRIOT;
import org.apache.jena.riot.WriterGraphRIOTFactory;

public class STTLWriterFactory implements WriterGraphRIOTFactory {

    @Override
    public WriterGraphRIOT create(RDFFormat syntaxForm) {
        return new STTLWriter() ;
    }

}
