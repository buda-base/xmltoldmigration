package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

public interface ResourceMigration {
	abstract public Model MigrateResource(Document xmlDocument);
}
