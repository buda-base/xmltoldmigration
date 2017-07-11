# XSD Files

This directory contains a modified copy of the xsd files at the origin of the DKMS tool, so corresponding more or less to the data format in the eXist database. They have been modified to match the data more accurately so that the migration can capture all the data, even corner cases.

### Checking XML dump against the xsd files

You can launch from the directory

    ./check-db-xml-files.sh X

where `X` is a type (work, person, outline, etc.). This will compute a file named `xml-X-validation.log` containing the different validation errors. Note that you must have [xmllint](http://xmlsoft.org/xmllint.html) installed.
