#!/usr/bin/env bash

function lint {
	echo '' > xml-$1-validation.log
 	for f in ../../../../tbrc/tbrc-$1s/*.xml; do
 		xmllint --noout $f --schema $1.xsd 2>&1 | head -n -1 >> xml-$1-validation.log; done
}

lint $1
# work, person, place, corporation, imagegroup, lineage, topic, pubinfo, product, scanrequest, office, outline
